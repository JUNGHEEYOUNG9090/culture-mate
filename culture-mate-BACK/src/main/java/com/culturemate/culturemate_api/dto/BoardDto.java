package com.culturemate.culturemate_api.dto;

import com.culturemate.culturemate_api.domain.community.Board;
import com.culturemate.culturemate_api.domain.event.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


public class BoardDto {

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "BoardRequest", description = "게시물 생성/수정 요청 DTO")
  public static class Request {
    @Schema(description = "게시물 제목", example = "새로운 전시회 정보 공유", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "게시물 내용", example = "이번에 새로 열린 전시에요...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "작성자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "작성자 정보는 필수입니다.")
    private Long authorId;

    @Schema(description = "연관 이벤트 타입", example = "EXHIBITION")
    private EventType eventType;

    @Schema(description = "연관 이벤트 ID", example = "101")
    private Long eventId;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "BoardResponse", description = "게시물 상세 정보 응답 DTO")
  public static class Response {
    private Long id;
    private String title;
    private String content;
    private MemberDto.ProfileResponse author;
    private EventDto.ResponseCard eventCard;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Response from(Board board) {
      return Response.builder()
        .id(board.getId())
        .title(board.getTitle())
        .content(board.getContent())
        .author(MemberDto.ProfileResponse.from(board.getAuthor()))
        .eventCard(board.getEvent() != null ? EventDto.ResponseCard.from(board.getEvent(), false) : null)
        .likeCount(board.getLikeCount())
        .commentCount(board.getCommentCount())
        .createdAt(board.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
        .updatedAt(board.getUpdatedAt() != null ?
          board.getUpdatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
        .build();
    }
  }
}
