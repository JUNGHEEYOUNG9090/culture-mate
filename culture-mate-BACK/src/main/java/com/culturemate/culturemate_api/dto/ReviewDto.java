package com.culturemate.culturemate_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.culturemate.culturemate_api.domain.event.EventReview;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class ReviewDto {

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "ReviewRequest")
  public static class Request {

    @NotNull(message = "이벤트 ID는 필수입니다")
    private Long eventId;

    @NotNull(message = "평점은 필수입니다")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
    private Integer rating;

    private String content;  // null 허용 (리뷰 내용은 선택사항)

  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "ReviewResponse", description = "리뷰 정보 응답 DTO")
  public static class Response {

    private Long id;
    private Long eventId;
    private MemberDto.ProfileResponse author;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Response from(EventReview eventReview) {
      return Response.builder()
        .id(eventReview.getId())
        .eventId(eventReview.getEvent().getId())
        .author(MemberDto.ProfileResponse.from(eventReview.getMember()))
        .rating(eventReview.getRating())
        .content(eventReview.getContent())
        .createdAt(eventReview.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
        .updatedAt(eventReview.getUpdatedAt() != null ?
          eventReview.getUpdatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
        .build();
    }

  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "ReviewResponseWithEvent")
  public static class ResponseWithEvent {

    private Long id;
    private EventDto.ResponseCard event;
    private MemberDto.ProfileResponse author;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ResponseWithEvent from(EventReview eventReview) {
      return ResponseWithEvent.builder()
        .id(eventReview.getId())
        .event(EventDto.ResponseCard.from(eventReview.getEvent(), false))
        .author(MemberDto.ProfileResponse.from(eventReview.getMember()))
        .rating(eventReview.getRating())
        .content(eventReview.getContent())
        .createdAt(eventReview.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
        .updatedAt(eventReview.getUpdatedAt() != null ?
          eventReview.getUpdatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
        .build();
    }

  }

}