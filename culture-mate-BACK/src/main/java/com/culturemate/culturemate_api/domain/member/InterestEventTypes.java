package com.culturemate.culturemate_api.domain.member;

import com.culturemate.culturemate_api.domain.event.EventType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "member_interest_event_types")
public class InterestEventTypes {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "member_interest_event_type_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_detail_id", nullable = false)
  private MemberDetail memberDetail;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  private EventType eventType;

  // 추가 메타데이터 (필요시)
  @Column(name = "priority")
  private Integer priority; // 관심도 우선순위
}