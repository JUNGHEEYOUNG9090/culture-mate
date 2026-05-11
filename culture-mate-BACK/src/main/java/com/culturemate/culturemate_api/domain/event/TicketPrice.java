package com.culturemate.culturemate_api.domain.event;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketPrice {
  @Id
  @GeneratedValue
  @Column(name = "ticket_price_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id" ,nullable = false)
  private Event event;

  @Setter
  private String ticketType;   // 티켓타입
  
  @Setter
  private Integer price;        // 가격

}
