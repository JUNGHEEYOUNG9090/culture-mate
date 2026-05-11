package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.member.MemberDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberDetailRepository extends JpaRepository<MemberDetail, Long> {

  // 이메일 중복 검증
  boolean existsByEmail(String email);

  // N+1 문제 해결: 관심 이벤트 타입 조회
  @Query("""
      SELECT DISTINCT md
      FROM MemberDetail md
      LEFT JOIN FETCH md.interestEventTypes
      WHERE md.id = :memberId
      """)
  Optional<MemberDetail> findByIdWithEventTypes(@Param("memberId") Long memberId);

  // N+1 문제 해결: 관심 태그 조회
  @Query("""
      SELECT DISTINCT md
      FROM MemberDetail md
      LEFT JOIN FETCH md.interestTags it
      LEFT JOIN FETCH it.tag
      WHERE md.id = :memberId
      """)
  Optional<MemberDetail> findByIdWithTags(@Param("memberId") Long memberId);
}
