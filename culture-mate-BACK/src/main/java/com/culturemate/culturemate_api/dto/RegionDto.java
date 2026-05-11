package com.culturemate.culturemate_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.culturemate.culturemate_api.domain.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 지역 관련 DTO 컨테이너
 */
public class RegionDto {

  /**
   * 지역 요청/검색을 위한 DTO
   */
  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "RegionRequest")
  public static class Request {
    private String level1;
    private String level2;
    private String level3;

    public boolean hasRegion() {
      return level1 != null && 
             !level1.trim().isEmpty() && 
             !"전체".equals(level1.trim());
    }
  }

  /**
   * 지역 응답을 위한 Response DTO
   */
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "RegionResponse", description = "지역 정보 응답 DTO")
  public static class Response {
    private Long id;
    private String regionName;
    private String level1;
    private String level2;
    private String level3;
    private String fullPath;  // 호환성을 위해 유지, 동적 생성

    public static Response from(Region region) {
      if (region == null) {
        return null;
      }

      // 계층구조를 따라 올라가면서 level1, level2, level3 구성
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

      return Response.builder()
        .id(region.getId())
        .regionName(region.getRegionName())
        .level1(level1)
        .level2(level2)
        .level3(level3)
        .fullPath(buildFullPath(level1, level2, level3))
        .build();
    }

    private static String buildFullPath(String level1, String level2, String level3) {
      StringBuilder path = new StringBuilder();
      if (level1 != null) {
        path.append(level1);
      }
      if (level2 != null) {
        path.append(" > ").append(level2);
      }
      if (level3 != null) {
        path.append(" > ").append(level3);
      }
      return path.toString();
    }
  }
}