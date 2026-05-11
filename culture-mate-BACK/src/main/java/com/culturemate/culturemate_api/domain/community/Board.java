package com.culturemate.culturemate_api.domain.community;

import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.event.EventType;
import com.culturemate.culturemate_api.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Board {

  //=== 필드 ===//
  @Id @GeneratedValue
  @Column(name = "board_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  private EventType eventType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="event_id")
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="member_id", nullable = false)
  private Member author;

  @Column(nullable = false)
  @Setter
  private String title;

  @Column(length = 2000, nullable = false)
  @Setter
  private String content;

  @Column(nullable = false)
  private Instant createdAt;
  private Instant updatedAt;

  @Setter
  @Builder.Default
  private Integer likeCount = 0;
  // private Integer dislikeCount = 0;

  @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Comment> comments = new ArrayList<>();

  //=== 조회 로직 ===//
  public Integer getCommentCount() {
    return comments.size();
  }

  //=== 생성/수정 로직 ===//
  @PrePersist
  public void onCreate() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }

}
