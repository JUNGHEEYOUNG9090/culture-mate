package com.culturemate.culturemate_api.repository;

import com.culturemate.culturemate_api.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

  /**
   * 계층구조에서 정확한 Region 객체 하나를 찾기
   * level1, level2, level3 조합으로 특정 Region을 정확히 식별
   */
  @Query("SELECT r FROM Region r LEFT JOIN r.parent p1 LEFT JOIN p1.parent p2 WHERE " +
         "(:level3 IS NOT NULL AND " +
         " r.regionName = :level3 AND p1.regionName = :level2 AND p2.regionName = :level1) OR " +
         "(:level3 IS NULL AND :level2 IS NOT NULL AND " +
         " r.regionName = :level2 AND p1.regionName = :level1) OR " +
         "(:level3 IS NULL AND :level2 IS NULL AND :level1 IS NOT NULL AND " +
         " r.regionName = :level1 AND r.parent IS NULL)")
  Region findExactRegion(@Param("level1") String level1,
                        @Param("level2") String level2,
                        @Param("level3") String level3);

  /**
   * 특정 부모를 가지는 하위 지역들 조회
   */
  List<Region> findByParent(Region parent);

  /**
   * 특정 지역의 모든 하위 지역들만 조회 (자기 자신 제외)
   * NULL 처리 문제 해결을 위한 하위 지역 전용 쿼리
   */
  @Query("SELECT r FROM Region r WHERE " +
         "r.parent.id = :targetRegionId OR " +        // 직계 자식들
         "r.parent.parent.id = :targetRegionId")      // 손자들
  List<Region> findAllDescendants(@Param("targetRegionId") Long targetRegionId);


}