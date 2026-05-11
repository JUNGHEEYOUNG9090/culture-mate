package com.culturemate.culturemate_api.dto;

import com.culturemate.culturemate_api.domain.event.TicketPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketPriceDto {

  private String ticketType;
  private Integer price;

  public static TicketPriceDto from(TicketPrice ticketPrice) {
    return TicketPriceDto.builder()
            .ticketType(ticketPrice.getTicketType())
            .price(ticketPrice.getPrice())
            .build();
  }
}
