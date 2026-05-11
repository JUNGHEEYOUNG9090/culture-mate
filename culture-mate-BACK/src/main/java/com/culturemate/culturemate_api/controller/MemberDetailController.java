package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.MemberDetail;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.dto.MemberDto;
import com.culturemate.culturemate_api.domain.Image;
import com.culturemate.culturemate_api.domain.ImageTarget;
import com.culturemate.culturemate_api.service.AuthService;
import com.culturemate.culturemate_api.service.ImageService;
import com.culturemate.culturemate_api.service.ImagePermissionService;
import com.culturemate.culturemate_api.service.MemberDetailService;
import com.culturemate.culturemate_api.service.MemberService;
import com.culturemate.culturemate_api.service.TagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Member Detail API", description = "회원 상세 정보(마이페이지) 관리 API")
@RestController
@RequestMapping("/api/v1/member-detail")
@RequiredArgsConstructor
public class MemberDetailController {

  private final MemberDetailService memberDetailService;
  private final MemberService memberService;
  private final AuthService authService;
  private final ImageService imageService;
  private final ImagePermissionService imagePermissionService;
  private final TagService tagService;

  // ID 없이 호출 시 자신의 정보 조회 (관심사 포함)
  @GetMapping("")
  public ResponseEntity<MemberDto.DetailResponse> getMemberDetail(@AuthenticationPrincipal AuthenticatedUser user) {
    MemberDto.DetailResponse response = memberDetailService.findByMemberIdWithInterests(user.getMemberId());
    return ResponseEntity.ok(response);
  }

  // ID와 함께 호출 시 해당 회원 정보 조회 (관심사 포함)
  @GetMapping("/{memberId}")
  public ResponseEntity<MemberDto.DetailResponse> getMemberDetail(@PathVariable Long memberId) {
    MemberDto.DetailResponse response = memberDetailService.findByMemberIdWithInterests(memberId);
    return ResponseEntity.ok(response);
  }

  // 생성
  @PostMapping("/{memberId}")
  public ResponseEntity<MemberDto.DetailResponse> createMemberDetail(@PathVariable Long memberId,
                                                     @Valid @RequestBody MemberDto.DetailRequest dto) {
    Member member = memberService.findById(memberId);
    MemberDetail created = memberDetailService.create(member, dto);
    return ResponseEntity.status(201).body(MemberDto.DetailResponse.from(created));  // HTTP 201 Created + 데이터 반환
  }

  // 수정
  @PutMapping("/{memberId}")
  public ResponseEntity<MemberDto.DetailResponse> updateMemberDetail(@PathVariable Long memberId,
                                                       @Valid @RequestBody MemberDto.DetailRequest dto,
                                                       @AuthenticationPrincipal AuthenticatedUser requester) {
    authService.validateProfileAccess(memberId, requester.getMemberId());
    MemberDetail updated = memberDetailService.update(memberId, dto);
    return ResponseEntity.ok(MemberDto.DetailResponse.from(updated));  // HTTP 200 OK + 데이터 반환
  }

  // 삭제
  @DeleteMapping("/{memberId}")
  public ResponseEntity<Void> deleteMemberDetail(@PathVariable Long memberId,
                                                 @AuthenticationPrincipal AuthenticatedUser requester) {
    authService.validateProfileAccess(memberId, requester.getMemberId());
    memberDetailService.delete(memberId);
    return ResponseEntity.noContent().build();  // HTTP 204 No Content
  }

  // =========================== 이미지 관련 API ===========================

  // 회원 정보와 이미지 통합 업데이트
  @PutMapping("/{memberId}/with-images")
  public ResponseEntity<Void> updateMemberWithImages(@PathVariable Long memberId,
                                                     @RequestPart("data") String jsonData,
                                                     @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
                                                     @RequestPart(value = "backgroundImage", required = false) MultipartFile backgroundImage,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
    authService.validateProfileAccess(memberId, user.getMemberId());
    memberDetailService.updateWithImages(memberId, jsonData, profileImage, backgroundImage);
    return ResponseEntity.ok().build();
  }

  // 통합 이미지 업로드/수정 (프로필, 배경)
  @PatchMapping("/{memberId}/image")
  public ResponseEntity<Void> updateMemberImage(@PathVariable Long memberId,
                                          @RequestParam("image") MultipartFile imageFile,
                                          @RequestParam("type") String imageType,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
    switch (imageType.toLowerCase()) {
      case "profile":
        authService.validateProfileAccess(memberId, user.getMemberId());
        memberDetailService.updateProfileImage(memberId, imageFile);
        break;
      case "background":
        authService.validateProfileAccess(memberId, user.getMemberId());
        memberDetailService.updateBackgroundImage(memberId, imageFile);
        break;
      default:
        throw new IllegalArgumentException("지원하지 않는 이미지 타입입니다: " + imageType + " (사용 가능: profile, background)");
    }
    return ResponseEntity.ok().build();
  }

  // 통합 이미지 삭제 (프로필, 배경)
  @DeleteMapping("/{memberId}/image")
  public ResponseEntity<Void> deleteMemberImage(@PathVariable Long memberId,
                                          @RequestParam("type") String imageType,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
    switch (imageType.toLowerCase()) {
      case "profile":
        authService.validateProfileAccess(memberId, user.getMemberId());
        memberDetailService.deleteProfileImage(memberId);
        break;
      case "background":
        authService.validateProfileAccess(memberId, user.getMemberId());
        memberDetailService.deleteBackgroundImage(memberId);
        break;
      default:
        throw new IllegalArgumentException("지원하지 않는 이미지 타입입니다: " + imageType + " (사용 가능: profile, background)");
    }
    return ResponseEntity.noContent().build();
  }

  // ===== 갤러리 관리 =====

  // 갤러리 이미지 업로드 (다중)
  @PostMapping("/{memberId}/gallery")
  public ResponseEntity<Void> uploadMemberGalleryImages(@PathVariable Long memberId,
                                                 @RequestParam("images") List<MultipartFile> images) {
    imageService.uploadMultipleImages(images, ImageTarget.MEMBER_GALLERY, memberId);
    return ResponseEntity.status(201).build();
  }

  // 갤러리 이미지 목록 조회 (경로만 반환)
  @GetMapping("/{memberId}/gallery")
  public ResponseEntity<List<String>> getMemberGalleryImages(@PathVariable Long memberId) {
    List<Image> images = imageService.getImagesByTargetTypeAndId(ImageTarget.MEMBER_GALLERY, memberId);
    List<String> imagePaths = images.stream()
        .map(Image::getPath)
        .toList();
    return ResponseEntity.ok(imagePaths);
  }

  // 갤러리 이미지 삭제 (경로 기반)
  @DeleteMapping("/{memberId}/gallery")
  public ResponseEntity<Void> deleteMemberGalleryImage(@PathVariable Long memberId,
                                                 @RequestParam String imagePath) {
    // 1. 권한 검증
    imagePermissionService.validateDeletePermission(imagePath, memberId);
    
    // 2. 이미지 삭제
    imageService.deleteImageByPath(imagePath);
    
    return ResponseEntity.noContent().build();
  }

  // 갤러리 이미지 전체 삭제
  @DeleteMapping("/{memberId}/gallery/all")
  public ResponseEntity<Void> deleteAllMemberGalleryImages(@PathVariable Long memberId) {
    Long requesterId = memberId; // 명확성을 위해 변수명 분리
    
    // 1. 권한 검증
    imagePermissionService.validateDeleteAllPermission(ImageTarget.MEMBER_GALLERY, memberId, requesterId);
    
    // 2. 이미지 삭제
    imageService.deleteAllImagesByTarget(ImageTarget.MEMBER_GALLERY, memberId);
    
    return ResponseEntity.noContent().build();
  }

  // ===== 관심사 관리 API =====

  // 관심 이벤트 타입 업데이트 (전체 교체)
  @PutMapping("/{memberId}/interests/event-types")
  public ResponseEntity<Void> updateInterestEventTypes(@PathVariable Long memberId,
                                                       @RequestBody Map<String, List<String>> requestBody,
                                                       @AuthenticationPrincipal AuthenticatedUser requester) {
    List<String> eventTypes = requestBody.get("eventTypes");
    authService.validateProfileAccess(memberId, requester.getMemberId());
    memberDetailService.updateInterestEventTypes(memberId, eventTypes);
    return ResponseEntity.ok().build();
  }

  // 관심 태그 업데이트 (전체 교체)
  @PutMapping("/{memberId}/interests/tags")
  public ResponseEntity<Void> updateInterestTags(@PathVariable Long memberId,
                                                 @RequestBody Map<String, List<String>> requestBody,
                                                 @AuthenticationPrincipal AuthenticatedUser requester) {
    List<String> tags = requestBody.get("tags");
    authService.validateProfileAccess(memberId, requester.getMemberId());
    memberDetailService.updateInterestTags(memberId, tags);
    return ResponseEntity.ok().build();
  }

}
