package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.Image;
import com.culturemate.culturemate_api.domain.ImageTarget;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImagePermissionService {

  private final ImageRepository imageRepository;
  private final MemberService memberService;

  /**
   * 이미지 삭제 권한 검증 (경로 기반)
   *
   * @param webPath 이미지 웹 경로
   * @param requesterId 요청자 ID
   * @throws IllegalArgumentException 권한이 없거나 이미지를 찾을 수 없는 경우
   */
  public void validateDeletePermission(String webPath, Long requesterId) {
    // 1. 이미지 정보 조회
    Image image = imageRepository.findByPath(webPath)
        .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다: " + webPath));

    // 2. 권한 검증
    validateDeletePermission(image, requesterId);
  }

  /**
   * 이미지 삭제 권한 검증 (Image 객체 기반)
   *
   * @param image 이미지 객체
   * @param requesterId 요청자 ID
   * @throws IllegalArgumentException 권한이 없는 경우
   */
  public void validateDeletePermission(Image image, Long requesterId) {
    // ADMIN은 모든 이미지 삭제 가능
    Member requester = memberService.findById(requesterId);
    if (requester.getRole() == Role.ADMIN) {
      return;
    }

    // 타입별 권한 검증
    switch (image.getTargetType()) {
      case MEMBER_GALLERY:
        // 본인의 갤러리 이미지인지 확인
        if (!image.getTargetId().equals(requesterId)) {
          throw new IllegalArgumentException("본인의 갤러리 이미지만 삭제할 수 있습니다");
        }
        break;
        
      case EVENT_CONTENT:
        // EVENT_CONTENT는 이벤트 ID를 받아서 처리
        // 추후 이벤트 작성자 권한 검증 추가 예정
        break;
        
      case BOARD_CONTENT:
        // 추후 게시글 작성자 권한 검증 추가 예정
        break;
        
      case CHAT_MESSAGE:
        // 추후 채팅 메시지 작성자 권한 검증 추가 예정
        break;
        
      default:
        // 현재는 MEMBER_GALLERY만 완전 지원
        throw new IllegalArgumentException("지원하지 않는 이미지 타입입니다: " + image.getTargetType());
    }
  }

  /**
   * 특정 타입의 이미지들에 대한 삭제 권한 검증
   *
   * @param targetType 이미지 타입
   * @param targetId 대상 ID
   * @param requesterId 요청자 ID
   * @throws IllegalArgumentException 권한이 없는 경우
   */
  public void validateDeleteAllPermission(ImageTarget targetType, Long targetId, Long requesterId) {
    // ADMIN은 모든 이미지 삭제 가능
    Member requester = memberService.findById(requesterId);
    if (requester.getRole() == Role.ADMIN) {
      return;
    }

    // 타입별 권한 검증
    switch (targetType) {
      case MEMBER_GALLERY:
        // 본인의 갤러리인지 확인
        if (!targetId.equals(requesterId)) {
          throw new IllegalArgumentException("본인의 갤러리 이미지만 삭제할 수 있습니다");
        }
        break;
        
      default:
        throw new IllegalArgumentException("지원하지 않는 이미지 타입입니다: " + targetType);
    }
  }

}