package com.culturemate.culturemate_api.domain.member;

import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.together.Together;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestTogethers {
  @Id
  @GeneratedValue
  @Column(name = "interest_together_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "together_id", nullable = false)
  private Together together;

  public InterestTogethers(Member member, Together together) {
    this.member = member;
    this.together = together;
  }
}
