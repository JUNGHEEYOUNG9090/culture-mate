package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.community.Board;
import com.culturemate.culturemate_api.domain.event.EventReview;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.domain.together.Together;
import com.culturemate.culturemate_api.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 중앙 집중식 권한 검증 서비스
 * - 순환 참조 방지
 * - 권한 검증 로직 중앙 관리
 * - 일관된 예외 메시지 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidationService {
  
  private final MemberRepository memberRepository;

  /**
   * 요청자 조회 (내부 헬퍼 메서드)
   * @param requesterId 요청자 ID
   * @return 검증된 Member 객체
   */
  private Member getRequester(Long requesterId) {
    return memberRepository.findById(requesterId)
      .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));
  }

  /**
   * 게시글 접근 권한 검증
   * @param board 게시글 객체
   * @param requesterId 요청자 ID
   */
  public void validateBoardAccess(Board board, Long requesterId) {
    Member requester = getRequester(requesterId);
    
    // ADMIN은 모든 게시글 수정/삭제 가능
    if (requester.getRole() == Role.ADMIN) {
      return;
    }
    
    // 일반 사용자는 본인 게시글만
    if (!board.getAuthor().getId().equals(requesterId)) {
      throw new IllegalArgumentException("본인의 게시글만 수정/삭제할 수 있습니다");
    }
  }

  /**
   * 모집글 접근 권한 검증
   * @param together 모집글 객체
   * @param requesterId 요청자 ID
   */
  public void validateTogetherAccess(Together together, Long requesterId) {
    Member requester = getRequester(requesterId);
    
    // ADMIN은 모든 모집글 수정/삭제 가능
    if (requester.getRole() == Role.ADMIN) {
      return;
    }
    
    // 일반 사용자는 본인이 호스트인 모집글만
    if (!together.getHost().getId().equals(requesterId)) {
      throw new IllegalArgumentException("본인이 호스트인 모집글만 수정/삭제할 수 있습니다");
    }
  }

  /**
   * 이벤트 리뷰 접근 권한 검증
   * @param eventReview 이벤트 리뷰 객체
   * @param requesterId 요청자 ID
   */
  public void validateEventReviewAccess(EventReview eventReview, Long requesterId) {
    Member requester = getRequester(requesterId);
    
    // ADMIN은 모든 리뷰 수정/삭제 가능
    if (requester.getRole() == Role.ADMIN) {
      return;
    }
    
    // 일반 사용자는 본인 리뷰만
    if (!eventReview.getMember().getId().equals(requesterId)) {
      throw new IllegalArgumentException("본인의 리뷰만 수정/삭제할 수 있습니다");
    }
  }

  /**
   * 이벤트 관리 권한 검증 (관리자 전용)
   * @param requesterId 요청자 ID
   */
  public void validateEventAdminAccess(Long requesterId) {
    Member requester = getRequester(requesterId);
    
    if (requester.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("이벤트 수정/삭제는 관리자만 가능합니다");
    }
  }

  /**
   * 프로필 접근 권한 검증
   * @param profileMemberId 프로필 소유자 ID
   * @param requesterId 요청자 ID
   */
  public void validateProfileAccess(Long profileMemberId, Long requesterId) {
    Member requester = getRequester(requesterId);
    
    // ADMIN은 모든 프로필 수정/삭제 가능
    if (requester.getRole() == Role.ADMIN) {
      return;
    }
    
    // 일반 사용자는 본인 프로필만
    if (!profileMemberId.equals(requesterId)) {
      throw new IllegalArgumentException("본인의 프로필만 수정/삭제할 수 있습니다");
    }
  }

  /**
   * 댓글 접근 권한 검증 (기존 CommentService용)
   * @param commentAuthorId 댓글 작성자 ID
   * @param requesterId 요청자 ID
   */
  public void validateCommentAccess(Long commentAuthorId, Long requesterId) {
    Member requester = getRequester(requesterId);
    
    // ADMIN은 모든 댓글 수정/삭제 가능
    if (requester.getRole() == Role.ADMIN) {
      return;
    }
    
    // 일반 사용자는 본인 댓글만
    if (!commentAuthorId.equals(requesterId)) {
      throw new IllegalArgumentException("본인의 댓글만 수정/삭제할 수 있습니다");
    }
  }

}