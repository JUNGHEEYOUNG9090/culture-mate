package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.Image;
import com.culturemate.culturemate_api.domain.ImageTarget;
import com.culturemate.culturemate_api.domain.Region;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventType;
import com.culturemate.culturemate_api.domain.event.TicketPrice;
import com.culturemate.culturemate_api.domain.member.InterestEvents;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.dto.EventDto;
import com.culturemate.culturemate_api.dto.EventSearchDto;
import com.culturemate.culturemate_api.dto.SearchResult;
import com.culturemate.culturemate_api.dto.TicketPriceDto;
import com.culturemate.culturemate_api.repository.EventRepository;
import com.culturemate.culturemate_api.repository.InterestEventsRepository;
import com.culturemate.culturemate_api.repository.MemberRepository;
import com.culturemate.culturemate_api.repository.TicketPriceRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class EventService {

  private final EventRepository eventRepository;
  private final TicketPriceRepository ticketPriceRepository;
  private final InterestEventsRepository interestEventsRepository;
  private final RegionService regionService;
  private final MemberService memberService;
  private final ImageService imageService;

  @Transactional
  public Event create(EventDto.Request requestDto, MultipartFile mainImage, List<MultipartFile> imagesToAdd) {
    Region region = requestDto.getRegion() != null ? regionService.findExact(requestDto.getRegion()) : null;

    String mainImagePath = null;
    String thumbnailImagePath = null;
    List<String> contentImagePaths = new ArrayList<>();

    try {
      // 1. 이미지 먼저 업로드
      if (mainImage != null && !mainImage.isEmpty()) {
        mainImagePath = imageService.uploadSingleImage(mainImage, ImageTarget.EVENT, "main");
        thumbnailImagePath = imageService.uploadThumbnail(mainImage, ImageTarget.EVENT);
      }

      // 2. 엔티티 생성 (이미지 경로 포함)
      Event event = Event.builder()
        .eventType(requestDto.getEventType())
        .title(requestDto.getTitle())
        .region(region)
        .eventLocation(requestDto.getEventLocation())
        .address(requestDto.getAddress())
        .addressDetail(requestDto.getAddressDetail())
        .startDate(requestDto.getStartDate())
        .endDate(requestDto.getEndDate())
        .durationMin(requestDto.getDurationMin())
        .minAge(requestDto.getMinAge())
        .description(requestDto.getDescription())
        .mainImagePath(mainImagePath)
        .thumbnailImagePath(thumbnailImagePath)
        .build();

      // 지역 스냅샷 동기화 (성능 최적화)
      event.updateRegionSnapshot(region);

      eventRepository.save(event); // DB 저장

      // 3. 티켓 가격 정보 저장
      if (requestDto.getTicketPrices() != null) {
        for (TicketPriceDto dto : requestDto.getTicketPrices()) {
          TicketPrice newTicketPrice = TicketPrice.builder()
            .event(event)
            .ticketType(dto.getTicketType())
            .price(dto.getPrice())
            .build();
          ticketPriceRepository.save(newTicketPrice);
        }
      }

      // 4. 설명 이미지 추가 및 경로 저장
      if (imagesToAdd != null && !imagesToAdd.isEmpty()) {
        contentImagePaths = uploadContentImages(event.getId(), imagesToAdd);
      }
      
      return event;

    } catch (Exception e) {
      // 5. 에러 발생 시 업로드된 파일 모두 삭제 (롤백)
      if (mainImagePath != null) {
        imageService.deletePhysicalFiles(mainImagePath, thumbnailImagePath);
      }
      if (!contentImagePaths.isEmpty()) {
        imageService.deletePhysicalFiles(contentImagePaths.toArray(new String[0]));
      }
      
      // 예외를 다시 던져서 DB 트랜잭션 롤백
      throw new RuntimeException("이벤트 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  public Event findById(Long eventId) {
    return eventRepository.findById(eventId)
      .orElseThrow(() -> new IllegalArgumentException("해당 이벤트가 존재하지 않습니다."));
  }

  // 상세 조회용 (엔티티 반환, Controller에서 DTO 변환)
  public Event findByIdWithDetails(Long eventId) {
    return findById(eventId);
  }

  // 기존 호환성 유지
  public List<Event> findAll() {
    return eventRepository.findAll();
  }

  // 페이지네이션과 정렬을 지원하는 전체 조회 (메인 메서드) - null 체크 추가
  public List<Event> findAll(Integer limit, Integer offset, String sortBy) {
    Sort sort = createEventSort(sortBy);

    // limit가 null이면 전체 조회 (기존 호환성 유지)
    if (limit == null) {
      return eventRepository.findAll(sort);
    }

    // 페이지네이션 적용
    int validOffset = offset != null ? offset : 0;
    Pageable pageable = PageRequest.of(validOffset / limit, limit, sort);
    return eventRepository.findAll(pageable).getContent();
  }

  // 오버로딩: 기본 정렬(latest) 사용
  public List<Event> findAll(Integer limit, Integer offset) {
    return findAll(limit, offset, "latest");
  }

  // Event 정렬 옵션 생성
  private Sort createEventSort(String sortBy) {
    return switch (sortBy) {
      case "popular" -> Sort.by("interestCount").descending();
      case "date" -> Sort.by("startDate").ascending();
      default -> Sort.by("createdAt").descending(); // "latest" 기본값
    };
  }

  // 통합 검색 메서드 (페이지네이션 + 정렬 지원)
  public SearchResult<Event> search(EventSearchDto searchDto, Integer limit, Integer offset, String sortBy) {
    List<Region> regions = null;
    if (searchDto.hasRegion()) {
      try {
        regions = regionService.findByHierarchy(searchDto.getRegion());
        // 빈 리스트면 null로 처리
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

    // 디버깅용 로그
    System.out.println("=== EventService 검색 파라미터 ===");
    System.out.println("keyword: " + (searchDto.hasKeyword() ? searchDto.getKeyword() : "null"));
    System.out.println("regions: " + regions);
    System.out.println("startDate: " + searchDto.getStartDate());
    System.out.println("endDate: " + searchDto.getEndDate());
    System.out.println("eventType: " + eventType);
    System.out.println("limit: " + limit + ", offset: " + offset + ", sortBy: " + sortBy);
    System.out.println("=================================");

    // 페이지네이션 및 정렬 적용 여부 결정
    if (limit != null && limit > 0) {
      Sort sort = createEventSort(sortBy);
      Pageable pageable = PageRequest.of(offset / limit, limit, sort);

      // 데이터 조회 (페이지네이션 적용)
      List<Event> events;
      long totalCount;

      if (regions == null || regions.isEmpty()) {
        // 지역 조건 없음
        events = eventRepository.findBySearchWithoutRegion(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType,
          pageable
        );

        // 전체 개수 조회 (동일한 조건)
        totalCount = eventRepository.countBySearchWithoutRegion(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType
        );
      } else {
        // 지역 조건 포함
        events = eventRepository.findBySearch(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          regions,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType,
          pageable
        );

        // 전체 개수 조회 (동일한 조건)
        totalCount = eventRepository.countBySearch(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          regions,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType
        );
      }

      return new SearchResult<>(events, totalCount);
    } else {
      // 기존 방식: 전체 조회 (SearchResult로 래핑)
      List<Event> events;
      if (regions == null || regions.isEmpty()) {
        events = eventRepository.findBySearchWithoutRegion(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType
        );
      } else {
        events = eventRepository.findBySearch(
          searchDto.hasKeyword() ? searchDto.getKeyword() : null,
          regions,
          searchDto.getStartDate(),
          searchDto.getEndDate(),
          eventType
        );
      }

      // limit/offset이 없는 경우 전체 개수는 실제 결과 개수와 동일
      return new SearchResult<>(events, events.size());
    }
  }

  // 기존 호출 호환용 오버로딩
  public List<Event> search(EventSearchDto searchDto) {
    return search(searchDto, null, null, "latest").getContent();
  }

  // 페이지네이션만 사용하는 오버로딩
  public List<Event> search(EventSearchDto searchDto, int limit, int offset) {
    return search(searchDto, limit, offset, "latest").getContent();
  }

  @Transactional
  public Event update(Long id, EventDto.Request requestDto, MultipartFile mainImage, List<MultipartFile> imagesToAdd, Long requesterId) {
    // 권한 검증: ADMIN만 이벤트 수정 가능
    validateAdminAccess(requesterId);
    
    Event event = findById(id);
    Region region = requestDto.getRegion() != null ? regionService.findExact(requestDto.getRegion()) : null;
    
    event.setEventType(requestDto.getEventType());
    event.setTitle(requestDto.getTitle());
    event.setRegion(region);
    event.setEventLocation(requestDto.getEventLocation());
    event.setAddress(requestDto.getAddress());
    event.setAddressDetail(requestDto.getAddressDetail());
    event.setStartDate(requestDto.getStartDate());
    event.setEndDate(requestDto.getEndDate());
    event.setDurationMin(requestDto.getDurationMin());
    event.setMinAge(requestDto.getMinAge());
    event.setDescription(requestDto.getDescription());

    // 티켓 가격 업데이트 로직
    updateTicketPrices(event, requestDto.getTicketPrices());

    // 메인 이미지 처리 (수정 시에는 기존 이미지 삭제 후 새 이미지 업로드)
    if (mainImage != null && !mainImage.isEmpty()) {
      try {
        // 기존 메인 이미지 삭제
        if (event.getThumbnailImagePath() != null || event.getMainImagePath() != null) {
          imageService.deletePhysicalFiles(event.getThumbnailImagePath(), event.getMainImagePath());
        }
        
        // 새 메인 이미지 업로드
        String mainImagePath = imageService.uploadSingleImage(mainImage, ImageTarget.EVENT, "main");
        String thumbnailImagePath = imageService.uploadThumbnail(mainImage, ImageTarget.EVENT);
        
        event.setMainImagePath(mainImagePath);
        event.setThumbnailImagePath(thumbnailImagePath);
      } catch (Exception e) {
        throw new RuntimeException("메인 이미지 업로드 중 오류가 발생했습니다: " + e.getMessage(), e);
      }
    }

    // 설명 이미지 삭제 처리
    if (requestDto.getImagesToDelete() != null && !requestDto.getImagesToDelete().isEmpty()) {
      for (String imagePath : requestDto.getImagesToDelete()) {
        deleteContentImage(event.getId(), imagePath);
      }
    }

    // 설명 이미지 추가 처리  
    if (imagesToAdd != null && !imagesToAdd.isEmpty()) {
      uploadContentImages(event.getId(), imagesToAdd);
    }
    
    return event;
  }

  private void updateTicketPrices(Event event, List<TicketPriceDto> ticketPriceDtos) {
    List<TicketPrice> existingTickets = ticketPriceRepository.findByEvent(event);
    Map<String, TicketPrice> existingTicketsMap = existingTickets.stream()
            .collect(Collectors.toMap(TicketPrice::getTicketType, t -> t));

    // request DTO 가 null 이거나 비어있다면, 해당 이벤트의 티켓가격 데이터 전체 삭제
    if (ticketPriceDtos == null || ticketPriceDtos.isEmpty()) {
        for (TicketPrice existingTicket : existingTickets) {
            ticketPriceRepository.delete(existingTicket);
        }
        return;
    }

    Set<String> dtoTicketTypes = ticketPriceDtos.stream()
            .map(TicketPriceDto::getTicketType)
            .collect(Collectors.toSet());

    // 1. DB에는 있지만 DTO에는 없는 티켓 삭제
    for (TicketPrice existingTicket : existingTickets) {
        if (!dtoTicketTypes.contains(existingTicket.getTicketType())) {
            ticketPriceRepository.delete(existingTicket);
        }
    }

    // 2. 타입명이 동일한 티켓은 가격데이터 확인후 업데이트. 없는건 새로 생성.
    for (TicketPriceDto dto : ticketPriceDtos) {
        TicketPrice ticketToUpdate = existingTicketsMap.get(dto.getTicketType());
        if (ticketToUpdate != null) {
            ticketToUpdate.setPrice(dto.getPrice());
        } else {
            TicketPrice newTicket = TicketPrice.builder()
                    .event(event)
                    .ticketType(dto.getTicketType())
                    .price(dto.getPrice())
                    .build();
            ticketPriceRepository.save(newTicket);
        }
    }
  }

  @Transactional
  public void delete(Long eventId, Long requesterId) {
    // 권한 검증: ADMIN만 이벤트 삭제 가능
    validateAdminAccess(requesterId);
    
    Event event = findById(eventId);
    
    // 관련 이미지들 삭제
    // 1. 메인 이미지 삭제
    if (event.getThumbnailImagePath() != null || event.getMainImagePath() != null) {
      imageService.deletePhysicalFiles(event.getThumbnailImagePath(), event.getMainImagePath());
    }
    
    // 2. 내용 이미지들 삭제
    imageService.deleteAllImagesByTarget(ImageTarget.EVENT_CONTENT, eventId);
    
    // 3. 이벤트 엔티티 삭제
    eventRepository.deleteById(eventId);
  }

  // 이벤트 관심 표시 토글
  @Transactional
  public boolean toggleEventInterest(Long eventId, Long memberId) {
    Event event = findById(eventId);  // EventService에서 조회
    Member member = memberService.findById(memberId);  // 의존성 주입된 MemberService 사용

    Optional<InterestEvents> existing = interestEventsRepository.findByMemberAndEvent(member, event);

    if (existing.isPresent()) {
      // 관심 표시 취소
      interestEventsRepository.delete(existing.get());
      eventRepository.updateInterestCount(eventId, -1); // 원자적 감소
      return false; // 취소됨
    } else {
      // 관심 표시 추가
      InterestEvents interestEvents = InterestEvents.builder()
        .member(member)
        .event(event)
        .build();
      interestEventsRepository.save(interestEvents);
      eventRepository.updateInterestCount(eventId, 1); // 원자적 증가
      return true; // 추가됨
    }
  }

  // =========================== 이미지 관련 메서드 ===========================
  // ℹ️ 메인 이미지 처리는 create/update 메서드에서 통합 처리

  // 이벤트 설명 이미지 업로드 (다중)
  @Transactional
  public List<String> uploadContentImages(Long eventId, List<MultipartFile> imageFiles) {
    Event event = findById(eventId);
    return imageService.uploadMultipleImages(imageFiles, ImageTarget.EVENT_CONTENT, eventId);
  }

  // 이벤트 설명 이미지 목록 조회
  public List<Image> getContentImages(Long eventId) {
    return imageService.getImagesByTargetTypeAndId(ImageTarget.EVENT_CONTENT, eventId);
  }

  // 컨텐츠 이미지 경로 목록 조회 (DTO 변환용 헬퍼 메서드)
  public List<String> getContentImagePaths(Long eventId) {
    return getContentImages(eventId).stream()
      .map(Image::getPath)
      .toList();
  }

  // 이벤트 설명 이미지 개별 삭제
  @Transactional
  public void deleteContentImage(Long eventId, String imagePath) {
    // 권한 검증은 컨트롤러에서 처리
    imageService.deleteImageByPath(imagePath);
  }

  // ℹ️ 전체 삭제는 update 메서드에서 imagesToDelete로 처리

  // 단일 이벤트 관심 여부 확인
  public boolean isInterested(Long eventId, Long memberId) {
    Event event = findById(eventId);
    Member member = memberService.findById(memberId);
    return interestEventsRepository.findByMemberAndEvent(member, event).isPresent();
  }

  // 배치 관심 여부 확인 (성능 최적화)
  public Map<Long, Boolean> getInterestStatusBatch(List<Long> eventIds, Long memberId) {
    List<InterestEvents> interests = interestEventsRepository
      .findByEventIdInAndMemberId(eventIds, memberId);
    
    Map<Long, Boolean> result = new HashMap<>();
    
    // 모든 이벤트를 false로 초기화
    eventIds.forEach(id -> result.put(id, false));
    
    // 관심 있는 이벤트만 true로 설정
    interests.forEach(interest -> 
      result.put(interest.getEvent().getId(), true));
    
    return result;
  }

  // 사용자 관심 이벤트 목록 조회
  public List<Event> getUserInterestEvents(Long memberId) {
    Member member = memberService.findById(memberId);
    List<InterestEvents> interestEvents = interestEventsRepository.findByMember(member);
    
    return interestEvents.stream()
      .map(InterestEvents::getEvent)
      .toList();
  }

  // ADMIN 권한 검증 메서드
  private void validateAdminAccess(Long requesterId) {
    Member requester = memberService.findById(requesterId);

    if (requester.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("이벤트 수정/삭제는 관리자만 가능합니다");
    }
  }

  // findRecentActive 메서드 제거됨 - 대신 search(empty, limit, 0, "latest") 사용

}