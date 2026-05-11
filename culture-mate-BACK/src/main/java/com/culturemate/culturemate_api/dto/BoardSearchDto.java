package com.culturemate.culturemate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSearchDto {

  private String keyword; // 제목이나 내용에서 검색할 키워드
  private String authorNickname; // 작성자 닉네임
  private Long eventId; // 특정 이벤트 ID
  private String eventType; // 이벤트 타입 (CONCERT, EXHIBITION 등)
  
  // 검증 및 유틸리티 메서드들
  public boolean hasKeyword() {
    return keyword != null && !keyword.trim().isEmpty();
  }
  
  public boolean hasAuthor() {
    return authorNickname != null && !authorNickname.trim().isEmpty();
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
  
  public boolean isEmpty() {
    return !hasKeyword() && !hasAuthor() && !hasEventId() && !hasEventType();
  }
}