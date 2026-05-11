package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.community.Board;
import com.culturemate.culturemate_api.domain.community.Comment;
import com.culturemate.culturemate_api.domain.community.CommentLike;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.dto.CommentDto;
import com.culturemate.culturemate_api.repository.CommentLikeRepository;
import com.culturemate.culturemate_api.repository.CommentRepository;
import com.culturemate.culturemate_api.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final BoardService boardService;
  private final MemberService memberService;

  // 댓글 생성
  @Transactional
  public Comment create(Long boardId, CommentDto.Request requestDto) {
    Board board = boardService.findById(boardId);
    Member author = memberService.findById(requestDto.getAuthorId());
    Comment parent = null;
    if (requestDto.getParentId() != null) {
      parent = commentRepository.findById(requestDto.getParentId())
        .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다."));
      
      // 2레벨 제한: 부모 댓글의 부모가 null이어야 함 (대댓글의 대댓글 방지)
      if (parent.getParent() != null) {
        throw new IllegalArgumentException("대댓글에는 댓글을 달 수 없습니다. 최대 2레벨까지만 가능합니다.");
      }
    }

    Comment comment = Comment.builder()
      .author(author)
      .board(board)
      .parent(parent)
      .content(requestDto.getComment())
      .likeCount(0)
      .build();

    return commentRepository.save(comment);
  }

  @Transactional
  public Comment update(Long commentId, CommentDto.Request requestDto, Long requesterId) {
    Comment comment = commentRepository.findById(commentId)
      .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
    
    validateCommentAccess(comment, requesterId);
    comment.setContent(requestDto.getComment());
    return comment;
  }


  // 특정 게시글의 부모 댓글만 조회 (replyCount 포함)
  public List<Comment> findParentCommentsByBoard(Long boardId) {
    return commentRepository.findByBoardIdAndParentIsNullOrderByCreatedAtDesc(boardId);
  }

  // 특정 댓글의 대댓글 조회
  public List<Comment> findReplies(Long parentId) {
    return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
  }

  // 삭제
  @Transactional
  public void delete(Long commentId, Long requesterId) {
    Comment comment = commentRepository.findById(commentId)
      .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
    
    validateCommentAccess(comment, requesterId);
    commentRepository.delete(comment);
  }

  @Transactional
  public boolean toggleCommentLike(Long commentId, Long memberId) {
    Comment comment = commentRepository.findById(commentId)
      .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
    Member member = memberService.findById(memberId);  // MemberService에서 조회

    Optional<CommentLike> existing = commentLikeRepository.findByCommentAndMember(comment, member);

    if (existing.isPresent()) {
      // 이미 좋아요 눌렀으면 취소
      commentLikeRepository.delete(existing.get());
      commentRepository.updateLikeCount(commentId, -1); // 원자적 감소
      return false; // 취소됨
    } else {
      // 좋아요 추가
      CommentLike commentLike = CommentLike.builder()
        .comment(comment)
        .member(member)
        .build();
      commentLikeRepository.save(commentLike);
      commentRepository.updateLikeCount(commentId, 1); // 원자적 증가
      return true; // 좋아요 성공
    }
  }

  // 권한 검증 메서드 (ADMIN 예외 처리 포함)
  private void validateCommentAccess(Comment comment, Long requesterId) {
    Member requester = memberService.findById(requesterId);
    
    // ADMIN은 모든 댓글 수정/삭제 가능
    if (requester.getRole() == Role.ADMIN) {
      return;
    }
    
    // 일반 사용자는 본인 댓글만
    if (!comment.getAuthor().getId().equals(requesterId)) {
      throw new IllegalArgumentException("본인의 댓글만 수정/삭제할 수 있습니다");
    }
  }

}
