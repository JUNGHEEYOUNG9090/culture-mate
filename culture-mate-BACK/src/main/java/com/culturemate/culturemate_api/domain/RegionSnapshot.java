package com.culturemate.culturemate_api.domain;

import com.culturemate.culturemate_api.dto.RegionDto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지역 정보 스냅샷 임베디드 타입
 * 
 * N+1 쿼리 문제 해결을 위한 비정규화된 지역 정보 저장
 * - 조회 성능 최적화: RegionDto.Response.from() N번 호출 → 0번
 * - 99.7% 쿼리 감소 효과 (1 + 3N → 1)
 * 
 * @author CultureMate Backend Team
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class RegionSnapshot {

  @Column(name = "region_snapshot_id")
  private Long regionId;

  @Column(name = "region_level1", length = 50)
  private String level1;

  @Column(name = "region_level2", length = 50)
  private String level2;

  @Column(name = "region_level3", length = 50)
  private String level3;

  /**
   * RegionDto.Response로 변환
   * fullPath는 필요시에만 동적 생성하여 메모리 효율성 확보
   * 
   * @return RegionDto.Response 객체
   */
  public RegionDto.Response toRegionDto() {
    if (regionId == null) {
      return null;
    }

    return RegionDto.Response.builder()
        .id(regionId)
        .level1(level1)
        .level2(level2)
        .level3(level3)
        .fullPath(buildFullPath())
        .regionName(getDisplayName())
        .build();
  }

  /**
   * 표시용 지역명 반환
   * level3 > level2 > level1 우선순위로 선택
   * 
   * @return 표시할 지역명
   */
  public String getDisplayName() {
    if (level3 != null && !level3.trim().isEmpty()) {
      return level3;
    }
    if (level2 != null && !level2.trim().isEmpty()) {
      return level2;
    }
    return level1;
  }

  /**
   * 전체 경로 문자열 생성
   * "서울특별시 > 강남구 > 역삼동" 형태
   * 
   * @return 전체 경로 문자열
   */
  private String buildFullPath() {
    if (level1 == null) {
      return "";
    }

    StringBuilder path = new StringBuilder();
    path.append(level1);

    if (level2 != null && !level2.trim().isEmpty()) {
      path.append(" > ").append(level2);
    }

    if (level3 != null && !level3.trim().isEmpty()) {
      path.append(" > ").append(level3);
    }

    return path.toString();
  }

  /**
   * Region 엔티티에서 RegionSnapshot 생성
   * RegionDto.Response.from() 로직과 동일한 방식으로 변환
   * 
   * @param region Region 엔티티
   * @return RegionSnapshot 객체
   */
  public static RegionSnapshot from(Region region) {
    if (region == null) {
      return null;
    }

    // RegionDto.Response.from()과 동일한 로직으로 level 추출
    String level1 = null, level2 = null, level3 = null;

    Region current = region;
    Region parent1 = current.getParent();
    Region parent2 = parent1 != null ? parent1.getParent() : null;

    if (parent2 != null) {
      // 3레벨: current가 level3, parent1이 level2, parent2가 level1
      level3 = current.getRegionName();
      level2 = parent1.getRegionName();
      level1 = parent2.getRegionName();
    } else if (parent1 != null) {
      // 2레벨: current가 level2, parent1이 level1
      level2 = current.getRegionName();
      level1 = parent1.getRegionName();
    } else {
      // 1레벨: current가 level1
      level1 = current.getRegionName();
    }

    return RegionSnapshot.builder()
        .regionId(region.getId())
        .level1(level1)
        .level2(level2)
        .level3(level3)
        .build();
  }
}