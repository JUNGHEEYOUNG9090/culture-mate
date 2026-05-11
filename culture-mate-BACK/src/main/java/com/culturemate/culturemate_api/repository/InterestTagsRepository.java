package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.member.InterestTags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterestTagsRepository extends JpaRepository<InterestTags, Long> {

  // 특정 태그의 사용 횟수
  @Query("SELECT COUNT(it) FROM InterestTags it WHERE it.tag.tag = :tagName")
  Long countByTag(@Param("tagName") String tagName);

  // 모든 태그별 사용 횟수 (인기 태그용)
  @Query("""
      SELECT it.tag.tag, COUNT(it)
      FROM InterestTags it
      GROUP BY it.tag.tag
      ORDER BY COUNT(it) DESC
      """)
  List<Object[]> findTagUsageCounts();

  // 상위 N개 인기 태그 (네이티브 쿼리로 LIMIT 구현)
  @Query(value = """
      SELECT t.tag, COUNT(*) as cnt
      FROM member_interest_tags mit
      JOIN tag t ON mit.tag_id = t.tag
      GROUP BY t.tag
      ORDER BY cnt DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<Object[]> findTopTagsWithCount(@Param("limit") int limit);

  // 특정 멤버의 관심 태그들 조회 (N+1 방지: Tag도 함께 페치)
  @Query("""
      SELECT DISTINCT it
      FROM InterestTags it
      LEFT JOIN FETCH it.tag
      WHERE it.memberDetail.id = :memberDetailId
      """)
  List<InterestTags> findByMemberDetailIdWithTag(@Param("memberDetailId") Long memberDetailId);

  // 특정 멤버의 관심 태그들 삭제
  void deleteByMemberDetailId(Long memberDetailId);
}