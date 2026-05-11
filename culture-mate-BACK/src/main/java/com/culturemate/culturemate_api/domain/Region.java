package com.culturemate.culturemate_api.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Region {

  //=== 필드 ===//
  @Id @GeneratedValue
  private long id;

  @ManyToOne(fetch = FetchType.LAZY)
  private Region parent; // 상위 지역

  private String regionName;

}
