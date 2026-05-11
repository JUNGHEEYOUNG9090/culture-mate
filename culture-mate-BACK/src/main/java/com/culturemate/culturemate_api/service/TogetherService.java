package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.Region;
import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventType;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.together.Participants;
import com.culturemate.culturemate_api.domain.together.ParticipationStatus;
import com.culturemate.culturemate_api.domain.together.Together;
import com.culturemate.culturemate_api.dto.*;

import java.time.ZoneId;
import com.culturemate.culturemate_api.exceptions.together.*;
import com.culturemate.culturemate_api.repository.ParticipantsRepository;
import com.culturemate.culturemate_api.repository.TogetherRepository;
import com.culturemate.culturemate_api.repository.InterestTogethersRepository;
import com.culturemate.culturemate_api.repository.ChatRoomRepository;
import com.culturemate.culturemate_api.domain.member.InterestTogethers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TogetherService {

  private final TogetherRepository togetherRepository;
  private final ParticipantsRepository participantsRepository;
  private final InterestTogethersRepository interestTogethersRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final MemberService memberService;
  private final RegionService regionService;
  private final EventService eventService;
  private final ImageService imageService;
  private final ChatRoomService chatRoomService;
  private final ValidationService validationService;


  /**
   * Together 생성 (완전한 Together 생성 - 호스트 참여자 포함)
   * - Together 엔티티 생성 및 저장
   * - 호스트를 자동으로 참여자(HOST 상태)로 추가
   * - 비즈니스 무결성: Together는 항상 호스트가 포함된 완전한 상태로 생성
   */
  @Transactional
  public Together create(Event event, Member host, Region region, TogetherDto.Request requestDto) {
    // 1. Together 엔티티 생성
    Together together = Together.builder()
      .event(event)
      .host(host)
      .title(requestDto.getTitle())
      .region(region)
      .meetingLocation(requestDto.getMeetingLocation())
      .meetingDate(requestDto.getMeetingDate())
      .maxParticipants(requestDto.getMaxParticipants())
      .content(requestDto.getContent())
      .build();

    // 지역 스냅샷 동기화 (성능 최적화)
    together.updateRegionSnapshot(region);

    // Together를 DB에 저장
    Together savedTogether = togetherRepository.save(together);

    // 호스트를 참여자로 자동 추가 (HOST 상태로)
    Participants hostParticipation = Participants.builder()
      .together(savedTogether)
      .participant(host)
      .status(ParticipationStatus.HOST)  // 호스트 상태로 설정
      .build();
    participantsRepository.save(hostParticipation);

    return savedTogether;
  }


  // 전체 조회 (페이지네이션 + 정렬 지원) - offset null 체크 추가
  public List<Together> findAll(Integer limit, Integer offset, String sortBy) {
    if (limit != null && limit > 0) {
      Sort sort = createTogetherSort(sortBy);
      int validOffset = offset != null ? offset : 0;
      Pageable pageable = PageRequest.of(validOffset / limit, limit, sort);
      return togetherRepository.findAll(pageable).getContent();
    } else {
      return togetherRepository.findAll(createTogetherSort(sortBy != null ? sortBy : "latest"));
    }
  }

  // 페이지네이션만 사용하는 오버로딩 (기본 정렬)
  public List<Together> findAll(Integer limit, Integer offset) {
    return findAll(limit, offset, "latest");
  }

  // 기존 호환성 유지용
  public List<Together> findAll() {
    return togetherRepository.findAll();
  }

  public Together findById(Long togetherId) {
    return togetherRepository.findById(togetherId)
        .orElseThrow(() -> new TogetherNotFoundException(togetherId));
  }

  // 해당 멤버가 호스트인 모집글
  public List<Together> findByHost(Member host) {
    return togetherRepository.findByHost(host);
  }
  // 호스트이든 동행인이든 상관없이 참여하는 동행을 불러옴 (모든 상태)
  public List<Together> findByMemberAll(Member member) {
    return togetherRepository.findByParticipantAll(member);
  }
  
  // 특정 상태의 신청 동행만 조회
  public List<Together> findByMemberAndStatus(Member member, String status) {
    ParticipationStatus participationStatus = ParticipationStatus.valueOf(status.toUpperCase());
    return togetherRepository.findByParticipantAndStatus(member, participationStatus);
  }

  // 통합 검색 기능 (페이지네이션 + 정렬 지원) - SearchResult 패턴 적용
  public SearchResult<Together> search(TogetherSearchDto searchDto, Integer limit, Integer offset, String sortBy) {
    List<Region> regions = null;
    if (searchDto.hasRegion()) {
      try {
        regions = regionService.findByHierarchy(searchDto.getRegion());
        if (regions != null && regions.isEmpty()) {
          regions = null;
        }
      } catch (Exception e) {
        System.out.println("Region 검색 중 오류: " + e.getMessage());
        regions = null;
      }
    }

    EventType eventType = null;
    if (searchDto.hasEventType()) {
      try {
        eventType = EventType.valueOf(searchDto.getEventType().toUpperCase());
      } catch (IllegalArgumentException e) {
        System.out.println("잘못된 EventType: " + searchDto.getEventType());
        eventType = null;
      }
    }

    // 페이지네이션 및 정렬 적용 여부 결정
    List<Together> results;
    if (limit != null && limit > 0) {
      Sort sort = createTogetherSort(sortBy);
      int validOffset = offset != null ? offset : 0;
      Pageable pageable = PageRequest.of(validOffset / limit, limit, sort);

      // 지역 조건에 따라 다른 Repository 메서드 사용 (Pageable 버전)
      if (regions == null || regions.isEmpty()) {
        results = togetherRepository.findBySearchWithoutRegion(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType,
          searchDto.getEventId(),
          pageable
        );
      } else {
        results = togetherRepository.findBySearch(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          regions,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType,
          searchDto.getEventId(),
          pageable
        );
      }
    } else {
      // 기존 방식: 전체 조회
      if (regions == null || regions.isEmpty()) {
        results = togetherRepository.findBySearchWithoutRegion(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType,
          searchDto.getEventId()
        );
      } else {
        results = togetherRepository.findBySearch(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          regions,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType,
          searchDto.getEventId()
        );
      }
    }

    // 페이지네이션 사용 시에만 카운트 조회
    long totalCount = 0;
    if (limit != null && limit > 0) {
      // 지역 조건에 따라 다른 카운트 쿼리 사용
      if (regions == null || regions.isEmpty()) {
        totalCount = togetherRepository.countBySearchWithoutRegion(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType,
          searchDto.getEventId()
        );
      } else {
        totalCount = togetherRepository.countBySearch(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          regions,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType,
          searchDto.getEventId()
        );
      }
    }

    // isActive 필터링 (Service에서 처리)
    if (searchDto.hasActiveFilter()) {
      Boolean activeFilter = searchDto.getIsActive();
      results = results.stream()
        .filter(together -> isActive(together) == activeFilter)
        .collect(Collectors.toList());
    }

    return new SearchResult<>(results, totalCount);
  }

  // 기존 호출 호환용 오버로딩
  public List<Together> search(TogetherSearchDto searchDto) {
    return search(searchDto, null, null, "latest").getContent();
  }

  // 페이지네이션만 사용하는 오버로딩
  public List<Together> search(TogetherSearchDto searchDto, int limit, int offset) {
    return search(searchDto, limit, offset, "latest").getContent();
  }

  // Together 정렬 옵션 생성
  private Sort createTogetherSort(String sortBy) {
    return switch (sortBy) {
      case "popular" -> Sort.by("interestCount").descending();
      case "date" -> Sort.by("meetingDate").ascending();
      default -> Sort.by("createdAt").descending(); // "latest" 기본값
    };
  }

  // 수정
  @Transactional
  public Together update(Long id, TogetherDto.Request requestDto, Long requesterId) {
    Together together = findById(id);
    
    // 권한 검증: 본인이 호스트인 모집글만 수정 가능
    validationService.validateTogetherAccess(together, requesterId);
    Event event = eventService.findById(requestDto.getEventId());
    Region region = regionService.findExact(requestDto.getRegion());

    // 날짜 검증 - 과거 날짜 방지
    if (together.getMeetingDate().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("모임 날짜는 오늘 이후여야 합니다");
    }

    // 참여자 수 검증 - 현재 참여자보다 적게 설정 방지
    Integer currentCount = together.getParticipantCount();
    if (together.getMaxParticipants() < currentCount) {
      throw new IllegalArgumentException("최대 참여자 수는 현재 참여자 수보다 적을 수 없습니다");
    }

    together.setEvent(event);
    together.setTitle(requestDto.getTitle());
    together.setRegion(region);
    together.setMeetingLocation(requestDto.getMeetingLocation());
    together.setMeetingDate(requestDto.getMeetingDate());
    together.setMaxParticipants(requestDto.getMaxParticipants());
    together.setContent(requestDto.getContent());
    
    return together;
  }

  @Transactional
  public void delete(Long togetherId, Long requesterId) {
    Together together = findById(togetherId);

    // 권한 검증: 본인이 호스트인 모집글만 삭제 가능
    validationService.validateTogetherAccess(together, requesterId);

    // 관련 엔티티들 수동 삭제 (외래키 제약조건 해결)
    // 삭제 순서가 중요: 자식 → 부모 순서로 삭제

    // 1. ChatRoom 삭제 (Together를 참조하는 주요 테이블)
    chatRoomRepository.deleteByTogetherId(togetherId);

    // 2. InterestTogethers 삭제 (관심 등록 정보)
    interestTogethersRepository.deleteByTogetherId(togetherId);

    // 3. Participants 삭제 (참여자 정보)
    participantsRepository.deleteByTogetherId(togetherId);

    // 4. 변경사항을 DB에 즉시 반영
    togetherRepository.flush();

    // 5. 이미지 파일들 삭제
    imageService.deletePhysicalFiles(together.getThumbnailImagePath(),
                                    together.getMainImagePath());

    // 6. 마지막으로 Together 엔티티 삭제
    togetherRepository.delete(together);
  }


  // ===== 참여자 관리 메서드 =====
  // 참여 여부 확인
  public boolean isParticipating(Long togetherId, Long memberId) {
    return participantsRepository.existsByTogetherIdAndParticipantId(togetherId, memberId);
  }


  // 모든 참여자 목록 조회 (상태 무관)
  public List<Member> getAllParticipants(Long togetherId) {
    List<Participants> participantsList = participantsRepository.findAllByTogetherId(togetherId);
    return participantsList.stream()
      .map(Participants::getParticipant)
      .collect(Collectors.toList());
  }
  
  // 특정 상태 참여자 목록 조회 (APPROVED 요청시 HOST도 포함)
  public List<Member> getParticipantsByStatus(Long togetherId, String status) {
    ParticipationStatus participationStatus = ParticipationStatus.valueOf(status.toUpperCase());

    // APPROVED 요청시 HOST도 함께 조회 (채팅방 참여 권한)
    List<ParticipationStatus> statusList;
    if (participationStatus == ParticipationStatus.APPROVED) {
      statusList = List.of(ParticipationStatus.APPROVED, ParticipationStatus.HOST);
    } else {
      statusList = List.of(participationStatus);
    }

    // 여러 상태를 한번에 조회
    List<Participants> participantsList = participantsRepository.findByTogetherIdAndStatusIn(togetherId, statusList);

    return participantsList.stream()
      .map(Participants::getParticipant)
      .collect(Collectors.toList());
  }

  // 동행 신청 (승인 대기 상태로 생성 + 신청용 채팅방 생성)
  @Transactional
  public Participants applyTogether(Long togetherId, Long memberId, String message) {
    Together together = findById(togetherId);

    if (!isActive(together)) {
      throw new TogetherClosedException(togetherId);
    }
    if (isParticipating(togetherId, memberId)) {
      throw new TogetherAlreadyJoinedException(togetherId, memberId);
    }

    Member applicant = memberService.findById(memberId);

    // 임시: 채팅방 연결 없이 신청만 저장 (스키마 업데이트 후 다시 활성화)
    Participants participation = Participants.builder()
        .together(together)
        .participant(applicant)
        .status(ParticipationStatus.PENDING)
        .message(message) // 신청 메시지 저장
        .build();

    Participants savedParticipation = participantsRepository.save(participation);

    // 신청용 1:1 채팅방 생성 (호스트-신청자 매칭) - 별도 처리
    try {
      ChatRoom applicationChatRoom = chatRoomService.createApplicationChatRoom(together, applicant);

      // 초기 신청 메시지 전송
      if (message != null && !message.trim().isEmpty()) {
        chatRoomService.sendMessage(applicationChatRoom.getId(), memberId, message);
      }

      // Participants 엔티티에 채팅방 연결 - 핵심 수정
      savedParticipation.setApplicationChatRoom(applicationChatRoom);
      participantsRepository.save(savedParticipation);

    } catch (Exception e) {
      // 채팅방 생성 실패 시에도 신청은 유지
      System.err.println("채팅방 생성 실패, 신청은 정상 처리: " + e.getMessage());
    }

    return savedParticipation;
  }

  // 동행 참여 거절
  @Transactional
  public void rejectParticipation(Long togetherId, Long participantId, Long requesterId) {
    Together together = findById(togetherId);
    if (!together.getHost().getId().equals(requesterId)) {
      throw new SecurityException("해당 모집글의 호스트가 아닙니다.");
    }

    Participants participation = participantsRepository.findByTogetherIdAndParticipantId(togetherId, participantId);
    if (participation == null) {
      throw new IllegalArgumentException("참여 신청을 찾을 수 없습니다.");
    }

    participation.setStatus(ParticipationStatus.REJECTED);
    participantsRepository.save(participation);
  }

  // 동행 참여 승인
  @Transactional
  public void approveParticipation(Long togetherId, Long participantId, Long requesterId) {
    Together together = findById(togetherId);
    if (!together.getHost().getId().equals(requesterId)) {
      throw new SecurityException("해당 모집글의 호스트가 아닙니다.");
    }

    // 승인 시점에도 모집 가능 상태 확인 (정원, 날짜, 호스트 설정)
    if (!isActive(together)) {
      throw new TogetherClosedException(togetherId);
    }

    Participants participation = participantsRepository.findByTogetherIdAndParticipantId(togetherId, participantId);
    if (participation == null) {
      throw new IllegalArgumentException("참여 신청을 찾을 수 없습니다.");
    }

    participation.setStatus(ParticipationStatus.APPROVED);
    participantsRepository.save(participation);

    // 승인 시 참여자 수 증가
    togetherRepository.updateParticipantCount(togetherId, 1);

    // 그룹 채팅방 처리: 없으면 생성하고, 승인된 참여자를 추가 (UI는 열지 않음)
    ensureGroupChatRoomAndAddMember(together, participantId);
  }

  /**
   * 그룹 채팅방 존재 확인 및 승인된 참여자 추가
   * - 그룹 채팅방이 없으면 생성
   * - 승인된 참여자를 그룹 채팅방에 추가
   * - UI는 자동으로 열지 않음 (백엔드 로직만)
   */
  private void ensureGroupChatRoomAndAddMember(Together together, Long participantId) {
    try {
      // 기존 그룹 채팅방 조회 시도
      ChatRoom groupChatRoom = chatRoomService.findGroupChatByTogether(together);

      // 그룹 채팅방이 존재하면 승인된 참여자 추가
      chatRoomService.addMemberToRoom(groupChatRoom.getId(), participantId);

    } catch (IllegalArgumentException e) {
      // 그룹 채팅방이 없는 경우 생성
      ChatRoom newGroupChatRoom = chatRoomService.createChatRoom(together);

      // 호스트를 그룹 채팅방에 추가
      chatRoomService.addMemberToRoom(newGroupChatRoom.getId(), together.getHost().getId());

      // 모든 승인된 참여자들을 그룹 채팅방에 추가
      List<Member> approvedParticipants = getParticipantsByStatus(together.getId(), "APPROVED");
      for (Member participant : approvedParticipants) {
        chatRoomService.addMemberToRoom(newGroupChatRoom.getId(), participant.getId());
      }

      // 현재 승인된 참여자도 추가 (위 루프에 포함될 수 있지만 중복 방지)
      chatRoomService.addMemberToRoom(newGroupChatRoom.getId(), participantId);
    }
  }

  // 동행 참여 취소
  @Transactional
  public void leaveTogether(Long togetherId, Long memberId) {
    Participants participation = participantsRepository
        .findByTogetherIdAndParticipantId(togetherId, memberId);

    if (participation == null) {
      throw new TogetherNotJoinedException(togetherId, memberId);
    }

    Together together = findById(togetherId);
    if(together.getMeetingDate().isBefore(LocalDate.now())) {
      throw new TogetherExpiredException(togetherId, together.getMeetingDate());
    }

    // 이미 취소된 상태인지 확인
    if (participation.getStatus() == ParticipationStatus.CANCELED) {
      throw new IllegalStateException("이미 취소된 참여 신청입니다");
    }

    // 승인된 상태였다면 참여자 수 감소
    boolean wasApproved = participation.getStatus() == ParticipationStatus.APPROVED;

    // 참여 상태를 CANCELED로 변경
    participation.setStatus(ParticipationStatus.CANCELED);
    participantsRepository.save(participation);

    // 승인된 상태였다면 참여자 수 감소
    if (wasApproved) {
      togetherRepository.updateParticipantCount(togetherId, -1);
    }

    // 채팅방 나가기
    chatRoomService.removeMemberFromRoomByTogether(together, memberId);
  }

  // 호스트의 멤버 강제 퇴출
  @Transactional
  public void removeMember(Long togetherId, Long participantId, Long hostId) {
    Together together = findById(togetherId);
    
    // 호스트 권한 확인
    if (!together.getHost().getId().equals(hostId)) {
      throw new SecurityException("해당 모집글의 호스트가 아닙니다.");
    }
    
    // 참여자 존재 확인
    Participants participation = participantsRepository.findByTogetherIdAndParticipantId(togetherId, participantId);
    if (participation == null) {
      throw new IllegalArgumentException("해당 참여자를 찾을 수 없습니다.");
    }
    
    // 호스트는 자기 자신을 내보낼 수 없음
    if (hostId.equals(participantId)) {
      throw new IllegalArgumentException("호스트는 자신을 내보낼 수 없습니다.");
    }
    
    // 승인된 상태였다면 참여자 수 감소
    boolean wasApproved = participation.getStatus() == ParticipationStatus.APPROVED;
    
    // 참여 상태를 REJECTED로 변경
    participation.setStatus(ParticipationStatus.REJECTED);
    
    if (wasApproved) {
      togetherRepository.updateParticipantCount(togetherId, -1);
    }
    
    // 채팅방에서 제거
    chatRoomService.removeMemberFromRoomByTogether(together, participantId);
  }

  // ===== 상태 관리 메서드 =====

  // 실제 모집 가능 여부 확인 (종합 판단)
  public boolean isActive(Together together) {
    // 1. 호스트가 모집을 비활성화한 경우
    if (!together.isHostRecruitingEnabled()) {
      return false;
    }
    
    // 2. 날짜가 지난 경우
    if (together.getMeetingDate().isBefore(LocalDate.now())) {
      return false;
    }
    
    // 3. 정원이 다 찬 경우
    if (together.getParticipantCount() >= together.getMaxParticipants()) {
      return false;
    }
    
    return true;
  }
  
  // 오버로드 메서드
  public boolean isActive(Long togetherId) {
    Together together = findById(togetherId);
    return isActive(together);
  }

  @Transactional
  public void closeTogether(Long togetherId, Long requesterId) {
    Together together = findById(togetherId);
    if (!together.getHost().getId().equals(requesterId)) {
      throw new SecurityException("호스트만 모집을 종료할 수 있습니다.");
    }
    together.setHostRecruitingEnabled(false);
  }

  @Transactional
  public void reopenTogether(Long togetherId, Long requesterId) {
    Together together = findById(togetherId);
    if (!together.getHost().getId().equals(requesterId)) {
      throw new SecurityException("호스트만 모집을 재개할 수 있습니다.");
    }
    together.setHostRecruitingEnabled(true);
  }

  // DTO 생성 헬퍼 메서드 (관심 여부 미포함)
  public TogetherDto.Response toResponseDto(Together together) {
    return toResponseDto(together, false);
  }

  // DTO 생성 헬퍼 메서드 (관심 여부 포함)
  public TogetherDto.Response toResponseDto(Together together, boolean isInterested) {
    return TogetherDto.Response.builder()
      .id(together.getId())
      .event(EventDto.ResponseCard.from(together.getEvent(), false))
      .host(MemberDto.ProfileResponse.from(together.getHost()))
      .title(together.getTitle())
      .region(together.getRegionSnapshot() != null ? 
              together.getRegionSnapshot().toRegionDto() : 
              null)  // 🚀 N+1 쿼리 문제 해결: 스냅샷 사용
      .meetingLocation(together.getMeetingLocation())
      .meetingDate(together.getMeetingDate())
      .maxParticipants(together.getMaxParticipants())
      .currentParticipants(together.getParticipantCount())
      .interestCount(together.getInterestCount()) // 관심수 추가
      .content(together.getContent())
      .active(isActive(together)) // 실제 isActive 계산
      .isInterested(isInterested) // 관심 등록 여부
      .roomId(together.getGroupChatRoom() != null ? together.getGroupChatRoom().getId() : null) // 그룹 채팅방 ID
      .createdAt(together.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
      .updatedAt(together.getUpdatedAt() != null ?
                 together.getUpdatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
      .build();
  }

  // ==================== 관심 등록 관련 메서드 ====================

  /**
   * 동행 관심 등록/해제 토글
   */
  @Transactional
  public boolean toggleInterest(Long togetherId, Long memberId) {
    Together together = findById(togetherId);
    Member member = memberService.findById(memberId);
    
    Optional<InterestTogethers> existingInterest = 
        interestTogethersRepository.findByMemberAndTogether(member, together);
    
    if (existingInterest.isPresent()) {
      // 이미 관심 등록되어 있음 -> 삭제 (관심 해제)
      interestTogethersRepository.delete(existingInterest.get());
      return false;
    } else {
      // 관심 등록되어 있지 않음 -> 생성 (관심 등록)
      InterestTogethers newInterest = new InterestTogethers(member, together);
      interestTogethersRepository.save(newInterest);
      return true;
    }
  }

  /**
   * 특정 회원이 관심 등록한 동행 목록 조회
   */
  public List<Together> getUserInterestTogethers(Long memberId) {
    Member member = memberService.findById(memberId);
    return interestTogethersRepository.findTogethersByMember(member);
  }

  /**
   * 특정 회원이 특정 동행에 관심 등록했는지 확인
   */
  public boolean isInterested(Long togetherId, Long memberId) {
    Together together = findById(togetherId);
    Member member = memberService.findById(memberId);
    return interestTogethersRepository.existsByMemberAndTogether(member, together);
  }

  /**
   * 여러 동행에 대한 회원의 관심 상태 배치 조회
   */
  public Map<Long, Boolean> getInterestStatusBatch(List<Long> togetherIds, Long memberId) {
    List<Long> interestedTogetherIds = interestTogethersRepository
        .findInterestedTogetherIdsByMemberIdAndTogetherIds(memberId, togetherIds);
    
    return togetherIds.stream()
        .collect(Collectors.toMap(
            id -> id,
            interestedTogetherIds::contains
        ));
  }

  // findRecentActive 메서드 제거됨 - 대신 search(empty, limit, 0, "latest") 사용


}
