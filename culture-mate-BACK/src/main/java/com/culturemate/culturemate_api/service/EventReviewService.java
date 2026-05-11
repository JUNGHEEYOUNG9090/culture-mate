package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventReview;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.dto.ReviewDto;
import com.culturemate.culturemate_api.exceptions.review.ReviewAlreadyExistsException;
import com.culturemate.culturemate_api.repository.EventRepository;
import com.culturemate.culturemate_api.repository.EventReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventReviewService {

  private final EventReviewRepository reviewRepository;
  private final EventRepository eventRepository;
  private final EventService eventService;
  private final MemberService memberService;
  private final ValidationService validationService;

  @Transactional
  public EventReview create(ReviewDto.Request reviewDto, Long authenticatedUserId) {
    Event event = eventService.findById(reviewDto.getEventId());
    Member member = memberService.findById(authenticatedUserId);  // 인증된 사용자 ID만 사용

    if (reviewRepository.existsByMemberAndEvent(member, event)) {
        throw new ReviewAlreadyExistsException("이미 이 이벤트에 대한 리뷰를 작성했습니다.");
    }
    
    EventReview eventReview = EventReview.builder()
      .event(event)
      .member(member)
      .rating(reviewDto.getRating())
      .content(reviewDto.getContent())
      .build();
    
    // 리뷰 저장 후 즉시 DB 반영 (평균별점 계산 시 새 리뷰가 포함되어야 함)
    EventReview savedReview = reviewRepository.saveAndFlush(eventReview);
    updateEventAverageRating(event.getId(), "create");
    return savedReview;
  }

  public List<EventReview> findByEventId(Long eventId) {
    Event event = eventService.findById(eventId);
    return reviewRepository.findByEvent(event);
  }

  public EventReview findById(Long reviewId) {
    return reviewRepository.findById(reviewId)
      .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
  }

  public List<EventReview> findAll() {
    return reviewRepository.findAll();
  }

  public List<EventReview> findByEvent(Event event) {
    return reviewRepository.findByEvent(event);
  }

  public List<EventReview> findByMember(Member member) {
    return reviewRepository.findByMember(member);
  }

  public List<EventReview> findByMemberId(Long memberId) {
    return reviewRepository.findByMemberId(memberId);
  }

  @Transactional
  public EventReview update(Long reviewId, ReviewDto.Request requestDto, Long authenticatedUserId) {
    EventReview existingReview = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
    
    // 권한 검증: 본인의 리뷰만 수정 가능
    validationService.validateEventReviewAccess(existingReview, authenticatedUserId);
    
    existingReview.setRating(requestDto.getRating());
    existingReview.setContent(requestDto.getContent());
    
    updateEventAverageRating(existingReview.getEvent().getId(), "update");
    return existingReview;
  }

  @Transactional
  public void delete(Long reviewId, Long authenticatedUserId) {
    System.out.println("리뷰 삭제 시도 ID: " + reviewId);
    EventReview eventReview = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. ID: " + reviewId));
    
    // 권한 검증: 본인의 리뷰만 삭제 가능
    validationService.validateEventReviewAccess(eventReview, authenticatedUserId);
    
    Long eventId = eventReview.getEvent().getId();
    reviewRepository.delete(eventReview);
    reviewRepository.flush();// 삭제를 즉시 DB 반영

    updateEventAverageRating(eventId, "delete");
  }

  // 리뷰 등록/삭제시 해당 이벤트 별점, 리뷰수 업데이트
  private void updateEventAverageRating(Long eventId, String type) {
    Event event = eventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
    
    event.recalculateAvgRating();
    if (type.equals("create")) {
      eventRepository.updateReviewCount(eventId, 1); // 원자적 증가
    } else if(type.equals("delete")) {
      eventRepository.updateReviewCount(eventId, -1); // 원자적 감소
    } else {}
  }

}