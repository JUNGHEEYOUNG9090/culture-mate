package com.culturemate.culturemate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TogetherSearchDto {

  private String keyword;
  private RegionDto.Request region;
  private String eventType;
  private Long eventId;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  private Boolean isActive; // null이면 전체, true면 모집중, false면 모집완료

  // 검증 및 유틸리티 메서드들
  public boolean hasKeyword() {
    return keyword != null && !keyword.trim().isEmpty();
  }
  
  public boolean hasRegion() {
    return region != null && region.hasRegion();
  }
  
  public boolean hasDateRange() {
    return startDate != null || endDate != null;
  }

  public boolean hasEventId() {
    return eventId != null;
  }

  public boolean hasEventType() {
    // 특정 이벤트를 지정하면 이벤트 타입에 대한 필터는 안함.
    if (this.hasEventId()) {
      return false;
    }
    return eventType != null && !eventType.trim().isEmpty();
  }

  public boolean hasActiveFilter() {
    return isActive != null;
  }

  public boolean isEmpty() {
    return !hasKeyword() && !hasRegion() && !hasDateRange() && !hasEventType() && !hasEventId() && !hasActiveFilter();
  }

}