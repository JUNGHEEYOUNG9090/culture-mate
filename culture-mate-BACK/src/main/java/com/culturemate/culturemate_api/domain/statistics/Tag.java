package com.culturemate.culturemate_api.domain.statistics;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Tag {
  @Id
  private String tag;

  // 태그 생성 헬퍼 메서드
  public static Tag createNew(String tagName) {
    return Tag.builder()
      .tag(tagName)
      .build();
  }
}
