package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.community.Board;
import com.culturemate.culturemate_api.domain.community.BoardLike;
import com.culturemate.culturemate_api.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
  Optional<BoardLike> findByBoardAndMember(Board board, Member member);
}
