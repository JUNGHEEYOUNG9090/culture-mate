package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.domain.community.Comment;
import com.culturemate.culturemate_api.dto.CommentDto;
import com.culturemate.culturemate_api.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Board Comment API", description = "댓글 관리 API")
@RestController
@RequestMapping("/api/v1/board/{boardId}/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @Operation(summary = "댓글 생성", description = "게시글에 댓글을 생성합니다")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "댓글 생성 성공"),
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
  })
  @PostMapping
  public ResponseEntity<CommentDto.Response> createComment(@Parameter(description = "게시글 ID", required = true) @PathVariable("boardId") Long boardId,
                                                          @Parameter(description = "댓글 내용", required = true) @RequestBody CommentDto.Request requestDto) {

    Comment created = commentService.create(boardId, requestDto);
    return ResponseEntity.status(201).body(CommentDto.Response.from(created)); // 201 Created
  }

  // 댓글 수정
  @PutMapping("/{commentId}")
  public ResponseEntity<CommentDto.Response> updateComment(@PathVariable Long commentId,
                                                          @RequestBody CommentDto.Request requestDto,
                                                          Authentication authentication) {
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
    Long requesterId = authenticatedUser.getMemberId();
    Comment updated = commentService.update(commentId, requestDto, requesterId);
    return ResponseEntity.ok(CommentDto.Response.from(updated)); // 200 OK
  }

  // 부모 댓글만 조회 (replyCount 포함)
  @GetMapping
  public ResponseEntity<List<CommentDto.Response>> getComments(@PathVariable Long boardId) {
    List<CommentDto.Response> comments = commentService.findParentCommentsByBoard(boardId)
      .stream()
      .map(CommentDto.Response::from)
      .collect(Collectors.toList());
    return ResponseEntity.ok(comments); // 200 OK
  }

  // 특정 댓글의 대댓글 조회
  @GetMapping("/{parentId}/replies")
  public ResponseEntity<List<CommentDto.Response>> getCommentReplies(@PathVariable Long parentId) {
    List<CommentDto.Response> replies = commentService.findReplies(parentId)
      .stream()
      .map(CommentDto.Response::from)
      .collect(Collectors.toList());
    return ResponseEntity.ok(replies); // 200 OK
  }

  // 삭제
    @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                            @PathVariable Long boardId,
                                            Authentication authentication) {
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
    Long requesterId = authenticatedUser.getMemberId();
    commentService.delete(commentId, requesterId);
    return ResponseEntity.noContent().build(); // 204 No Content
  }

  // 좋아요
  @PostMapping("/{commentId}/like")
  public ResponseEntity<String> toggleCommentLike(@PathVariable Long commentId,
                                                  @RequestParam Long memberId) {
    boolean liked = commentService.toggleCommentLike(commentId, memberId);

    if (liked) {
      return ResponseEntity.ok("댓글 좋아요 성공");
    } else {
      return ResponseEntity.ok("댓글 좋아요 취소");
    }
  }

}
