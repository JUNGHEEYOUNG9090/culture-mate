package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.ImageTarget;
import com.culturemate.culturemate_api.domain.event.EventType;
import com.culturemate.culturemate_api.domain.member.*;
import com.culturemate.culturemate_api.domain.statistics.Tag;
import com.culturemate.culturemate_api.dto.MemberDto;
import com.culturemate.culturemate_api.repository.MemberDetailRepository;
import com.culturemate.culturemate_api.repository.InterestEventTypesRepository;
import com.culturemate.culturemate_api.repository.InterestTagsRepository;
import com.culturemate.culturemate_api.repository.TagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberDetailService {
  private final MemberDetailRepository memberDetailRepository;
  private final InterestEventTypesRepository interestEventTypesRepository;
  private final InterestTagsRepository interestTagsRepository;
  private final ImageService imageService;
  private final TagRepository tagRepository;
  private final ObjectMapper objectMapper;

  // 이메일 중복 검증
  public boolean existsByEmail(String email) {
    return memberDetailRepository.existsByEmail(email);
  }

  // 회원 상세 조회 (관심사는 LAZY 로딩)
  public MemberDetail findByMemberId(Long memberId) {
    return memberDetailRepository.findById(memberId)
      .orElseThrow(() -> new IllegalArgumentException("회원 상세 정보가 존재하지 않습니다."));
  }

  // 관심사 포함 상세 조회 (3-쿼리 안전 접근법)
  public MemberDto.DetailResponse findByMemberIdWithInterests(Long memberId) {
    // 1. MemberDetail 기본 정보 조회
    MemberDetail memberDetail = findByMemberId(memberId);

    // 2. InterestEventTypes 별도 조회
    List<InterestEventTypes> eventTypes = interestEventTypesRepository.findByMemberDetailId(memberId);

    // 3. InterestTags 별도 조회 (Tag도 함께 페치)
    List<InterestTags> tags = interestTagsRepository.findByMemberDetailIdWithTag(memberId);

    // 4. DetailResponse 생성 (관심사 데이터 포함)
    return createDetailResponseWithInterests(memberDetail, eventTypes, tags);
  }

  // DetailResponse 생성 헬퍼 메서드
  private MemberDto.DetailResponse createDetailResponseWithInterests(
      MemberDetail memberDetail,
      List<InterestEventTypes> eventTypes,
      List<InterestTags> tags) {

    // 관심 이벤트 타입 매핑
    List<String> eventTypeNames = eventTypes.stream()
        .map(interest -> interest.getEventType().name())
        .toList();

    // 관심 태그 매핑
    List<String> tagNames = tags.stream()
        .map(interest -> interest.getTag().getTag())
        .collect(java.util.stream.Collectors.toList());

    // DetailResponse 생성 (from 메서드 대신 직접 빌더 사용)
    return MemberDto.DetailResponse.builder()
        .id(memberDetail.getId())
        .nickname(memberDetail.getNickname())
        .profileImagePath(memberDetail.getMainImagePath())
        .backgroundImagePath(memberDetail.getBackgroundImagePath())
        .intro(memberDetail.getIntro())
        .mbti(memberDetail.getMbti())
        .togetherScore(memberDetail.getTogetherScore())
        .email(memberDetail.getEmail())
        .visibility(memberDetail.getVisibility())
        .interestEventTypes(eventTypeNames)
        .interestTags(tagNames)
        .createdAt(memberDetail.getCreatedAt() != null ?
                   memberDetail.getCreatedAt().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
        .updatedAt(memberDetail.getUpdatedAt() != null ?
                   memberDetail.getUpdatedAt().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
        .build();
  }

  // 생성 (Member 객체와 DTO 사용) - @MapsId 활용
  public MemberDetail create(Member member, MemberDto.DetailRequest dto) {
    MemberDetail memberDetail = MemberDetail.builder()
      .member(member)  // @MapsId에 의해 ID 자동 매핑
      .nickname(dto.getNickname())
      .intro(dto.getIntro())
      .mbti(dto.getMbti())
      .email(dto.getEmail())
      .build();

    MemberDetail saved = memberDetailRepository.save(memberDetail);

    // 관심사가 있으면 업데이트
    if (dto.getInterestEventTypes() != null && !dto.getInterestEventTypes().isEmpty()) {
      updateInterestEventTypes(saved.getId(), dto.getInterestEventTypes());
    }
    if (dto.getInterestTags() != null && !dto.getInterestTags().isEmpty()) {
      updateInterestTags(saved.getId(), dto.getInterestTags());
    }

    return saved;
  }

  // ===== 관심사 관리 메서드 (전체 교체 방식) =====

  // 관심 이벤트 타입 업데이트
  public void updateInterestEventTypes(Long memberId, List<String> eventTypeStrings) {
    MemberDetail memberDetail = findByMemberId(memberId);

    // 기존 관심 이벤트 타입 모두 제거
    memberDetail.getInterestEventTypes().clear();

    // 새로운 관심 이벤트 타입 추가
    if (eventTypeStrings != null && !eventTypeStrings.isEmpty()) {
      List<InterestEventTypes> interestEventTypes = eventTypeStrings.stream()
        .map(eventTypeString -> {
          try {
            EventType eventType = EventType.valueOf(eventTypeString.toUpperCase());
            return InterestEventTypes.builder()
              .memberDetail(memberDetail)
              .eventType(eventType)
              .build();
          } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 이벤트 타입입니다: " + eventTypeString);
          }
        })
        .collect(Collectors.toList());

      memberDetail.getInterestEventTypes().addAll(interestEventTypes);
    }
  }

  // 관심 태그 업데이트 (전체 교체 방식)
  public void updateInterestTags(Long memberId, List<String> newTagNames) {
    MemberDetail memberDetail = findByMemberId(memberId);

    // 기존 관심 태그들 모두 제거
    memberDetail.getInterestTags().clear();

    // 새로운 관심 태그들 추가
    if (newTagNames != null && !newTagNames.isEmpty()) {
      // 중복 제거
      Set<String> uniqueTagNames = new HashSet<>(newTagNames);

      // 배치로 기존 태그들 조회 (N+1 문제 해결)
      List<Tag> existingTags = tagRepository.findByTagIn(new ArrayList<>(uniqueTagNames));
      Map<String, Tag> tagMap = existingTags.stream()
        .collect(Collectors.toMap(Tag::getTag, tag -> tag));

      // 없는 태그들 배치 생성
      Set<String> missingTagNames = uniqueTagNames.stream()
        .filter(tagName -> !tagMap.containsKey(tagName))
        .collect(Collectors.toSet());

      if (!missingTagNames.isEmpty()) {
        List<Tag> newTags = tagRepository.saveAll(
          missingTagNames.stream().map(Tag::createNew).collect(Collectors.toList())
        );
        newTags.forEach(tag -> tagMap.put(tag.getTag(), tag));
      }

      // InterestTags 생성
      List<InterestTags> newInterestTags = uniqueTagNames.stream()
        .map(tagName -> InterestTags.builder()
          .memberDetail(memberDetail)
          .tag(tagMap.get(tagName))
          .build())
        .collect(Collectors.toList());

      memberDetail.getInterestTags().addAll(newInterestTags);
    }
  }

  // 수정
  public MemberDetail update(Long memberId, MemberDto.DetailRequest dto) {
    MemberDetail memberDetail = memberDetailRepository.findById(memberId)
      .orElseThrow(() -> new IllegalArgumentException("회원 상세 정보가 존재하지 않습니다."));

    // 기본 필드 업데이트
    memberDetail.setNickname(dto.getNickname());
    memberDetail.setIntro(dto.getIntro());
    memberDetail.setMbti(dto.getMbti());
    memberDetail.setEmail(dto.getEmail());
    memberDetail.setVisibility(dto.getVisibility());

    // 관심사가 포함된 경우 함께 업데이트
    if (dto.getInterestEventTypes() != null) {
      updateInterestEventTypes(memberId, dto.getInterestEventTypes());
    }
    if (dto.getInterestTags() != null) {
      updateInterestTags(memberId, dto.getInterestTags());
    }

    return memberDetail;
  }

  // 삭제
  public void delete(Long memberId) {
    memberDetailRepository.deleteById(memberId);
  }

  // ===== 이미지 관리 =====

  // 프로필 이미지 업로드/수정 (썸네일 + 메인)
  public void updateProfileImage(Long memberId, MultipartFile imageFile) {
    MemberDetail memberDetail = findByMemberId(memberId);

    try {
      // 기존 이미지 삭제
      if (memberDetail.getThumbnailImagePath() != null || memberDetail.getMainImagePath() != null) {
        imageService.deletePhysicalFiles(memberDetail.getThumbnailImagePath(), memberDetail.getMainImagePath());
      }

      // 새 이미지 업로드
      String mainImagePath = imageService.uploadSingleImage(imageFile, ImageTarget.MEMBER_PROFILE, "main");
      String thumbnailImagePath = imageService.uploadThumbnail(imageFile, ImageTarget.MEMBER_PROFILE);

      // MemberDetail 업데이트
      memberDetail.setMainImagePath(mainImagePath);
      memberDetail.setThumbnailImagePath(thumbnailImagePath);

    } catch (Exception e) {
      throw new RuntimeException("프로필 이미지 업로드 실패: " + e.getMessage(), e);
    }
  }

  // 배경 이미지 업로드/수정
  public void updateBackgroundImage(Long memberId, MultipartFile imageFile) {
    MemberDetail memberDetail = findByMemberId(memberId);

    try {
      // 기존 이미지 삭제
      if (memberDetail.getBackgroundImagePath() != null) {
        imageService.deletePhysicalFiles(memberDetail.getBackgroundImagePath());
      }

      // 새 이미지 업로드 (배경은 메인만)
      String backgroundImagePath = imageService.uploadSingleImage(imageFile, ImageTarget.MEMBER_BACKGROUND, null);

      // MemberDetail 업데이트
      memberDetail.setBackgroundImagePath(backgroundImagePath);

    } catch (Exception e) {
      throw new RuntimeException("배경 이미지 업로드 실패: " + e.getMessage(), e);
    }
  }

  // 프로필 이미지 삭제
  public void deleteProfileImage(Long memberId) {
    MemberDetail memberDetail = findByMemberId(memberId);

    // 물리적 파일 삭제
    if (memberDetail.getThumbnailImagePath() != null || memberDetail.getMainImagePath() != null) {
      imageService.deletePhysicalFiles(memberDetail.getThumbnailImagePath(), memberDetail.getMainImagePath());
    }

    // DB 필드 초기화
    memberDetail.setThumbnailImagePath(null);
    memberDetail.setMainImagePath(null);
  }

  // 배경 이미지 삭제
  public void deleteBackgroundImage(Long memberId) {
    MemberDetail memberDetail = findByMemberId(memberId);

    // 물리적 파일 삭제
    if (memberDetail.getBackgroundImagePath() != null) {
      imageService.deletePhysicalFiles(memberDetail.getBackgroundImagePath());
    }

    // DB 필드 초기화
    memberDetail.setBackgroundImagePath(null);
  }

  // ===== 통합 업데이트 (텍스트 데이터 + 이미지) =====

  // 회원 정보와 이미지 통합 업데이트
  public void updateWithImages(Long memberId, String jsonData,
                              MultipartFile profileImage, MultipartFile backgroundImage) {
    try {
      // 1. JSON 데이터 파싱
      MemberDto.DetailRequest dto = objectMapper.readValue(jsonData, MemberDto.DetailRequest.class);

      // 2. 텍스트 데이터 업데이트
      update(memberId, dto);

      // 3. 프로필 이미지 업데이트 (파일이 있는 경우에만)
      if (profileImage != null && !profileImage.isEmpty()) {
        updateProfileImage(memberId, profileImage);
      }

      // 4. 배경 이미지 업데이트 (파일이 있는 경우에만)
      if (backgroundImage != null && !backgroundImage.isEmpty()) {
        updateBackgroundImage(memberId, backgroundImage);
      }

    } catch (Exception e) {
      throw new RuntimeException("회원 정보 통합 업데이트 실패: " + e.getMessage(), e);
    }
  }

}
