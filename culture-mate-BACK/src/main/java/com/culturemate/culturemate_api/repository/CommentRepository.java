package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.community.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  // 특정 게시글(Board)에 달린 댓글 조회 (N+1 해결: ManyToOne 관계만 포함)
  @EntityGraph(attributePaths = {"board", "parent"})
  List<Comment> findByBoardId(Long boardId);

  // 부모 댓글에 달린 대댓글 조회 (N+1 해결)
  @EntityGraph(attributePaths = {"board", "parent", "author"})
  List<Comment> findByParentId(Long parentId);
  
  // 부모 댓글에 달린 대댓글 조회 (오름차순)
  @EntityGraph(attributePaths = {"board", "parent", "author"})
  List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

  // 특정 게시글의 부모 댓글만 조회 (parent가 null인 댓글)
  @EntityGraph(attributePaths = {"board", "author"})
  List<Comment> findByBoardIdAndParentIsNullOrderByCreatedAtDesc(Long boardId);

  // 최신순 정렬 (게시글 기준) (N+1 해결) - 호환성 유지
  @EntityGraph(attributePaths = {"board", "parent", "author"})
  List<Comment> findByBoardIdOrderByCreatedAtDesc(Long boardId);

  // 원자적 좋아요 카운트 업데이트
  @Modifying
  @Query("UPDATE Comment c SET c.likeCount = c.likeCount + :increment WHERE c.id = :commentId")
  void updateLikeCount(@Param("commentId") Long commentId, @Param("increment") int increment);

}
