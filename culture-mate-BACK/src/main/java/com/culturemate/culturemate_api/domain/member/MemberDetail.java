package com.culturemate.culturemate_api.domain.member;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberDetail {
  @Id
  @Column(name = "member_id")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId // member도메인과 완전히 동일한 id값 매핑
  @JoinColumn(name = "member_id")
  private Member member;

  private String nickname;
  
  // 이미지 경로
  private String thumbnailImagePath;  // 프로필 썸네일 이미지 경로
  private String mainImagePath;       // 프로필 메인 이미지 경로  
  private String backgroundImagePath; // 배경 이미지 경로
  
  private String intro;
  private String mbti;
  private Integer togetherScore;
  private String email;

  // 관심 이벤트 타입과 태그
  @OneToMany(mappedBy = "memberDetail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<InterestEventTypes> interestEventTypes = new ArrayList<>();

  @OneToMany(mappedBy = "memberDetail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<InterestTags> interestTags = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  private VisibleType visibility;

  @Column(nullable = false)
  private Instant createdAt;
  private Instant updatedAt;

  @PrePersist
  public void onCreate() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }

}

