package com.culturemate.culturemate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchDto {
  
  private String keyword;
  private RegionDto.Request region;
  private String eventType;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

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
  
  public boolean hasEventType() {
    return eventType != null && !eventType.trim().isEmpty();
  }
  
  public boolean isEmpty() {
    return !hasKeyword() && !hasRegion() && !hasDateRange() && !hasEventType();
  }
  
}