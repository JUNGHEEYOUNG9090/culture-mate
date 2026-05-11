package com.culturemate.culturemate_api.domain.event;

import com.culturemate.culturemate_api.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventReview {
  @Id @GeneratedValue
  @Column(name = "review_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id" ,nullable = false)
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false)
  private Member member;

  @Column(nullable = false)
  private Integer rating;

  @Column(nullable = false, length = 2000)
  private String content;

  @Column(nullable = false)
  private Instant createdAt;
  private Instant updatedAt;

  //=== 생성/수정 로직 ===//
  public void setRating(Integer rating) {
    if(rating > 5) {
      this.rating = 5;
    } else if(rating < 1) {
      this.rating = 1;
    } else {
      this.rating = rating;
    }
  }

  @PrePersist
  public void onCreate() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public void setMember(Member member) {
    this.member = member;
  }

}
