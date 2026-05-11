package com.culturemate.culturemate_api.dto;

import com.culturemate.culturemate_api.domain.community.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class CommentDto {

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "CommentRequest")
  public static class Request {

    @NotNull(message = "게시물 ID는 필수입니다")
    private Long boardId;

    @NotNull(message = "작성자 ID는 필수입니다")
    private Long authorId;

    private Long parentId; // null이면 일반 댓글, 값이 있으면 대댓글

    @NotNull(message = "댓글 내용은 필수입니다")
    private String comment;

  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "CommentResponse", description = "댓글 상세 정보 응답 DTO")
  public static class Response {

    private Long id;
    private Long boardId;
    private MemberDto.ProfileResponse author;
    private String content;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    private Integer likeCount;
    private Integer replyCount; // 대댓글 수

    public static Response from(Comment comment) {
      return Response.builder()
        .id(comment.getId())
        .boardId(comment.getBoard().getId())
        .author(MemberDto.ProfileResponse.from(comment.getAuthor()))
        .content(comment.getContent())
        .createdAt(comment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
        .updatedAt(comment.getUpdatedAt() != null ? comment.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
        .likeCount(comment.getLikeCount())
        .replyCount(comment.getReplyCount())
        .build();
    }

  }

}