package com.culturemate.culturemate_api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {

  //=== 필드 ===//
  @Id @GeneratedValue
  @Column(name = "image_id")
  private long id;

  @Enumerated(EnumType.STRING)
  private ImageTarget targetType;

  private Long targetId;

  private String path;

  private Instant createdAt;

  //=== 생성자 ===//
  @Builder
  public Image(ImageTarget targetType, Long targetId, String path) {
    this.targetType = targetType;
    this.targetId = targetId;
    this.path = path;
  }

  //=== 메서드 ===//
  @PrePersist
  public void onCreate() {
    this.createdAt = Instant.now();
  }

}
