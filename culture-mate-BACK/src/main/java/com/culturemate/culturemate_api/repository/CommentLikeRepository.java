package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.community.Comment;
import com.culturemate.culturemate_api.domain.community.CommentLike;
import com.culturemate.culturemate_api.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
  Optional<CommentLike> findByCommentAndMember(Comment comment, Member member);
}
