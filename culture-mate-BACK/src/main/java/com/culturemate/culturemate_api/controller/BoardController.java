package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.dto.BoardDto;
import com.culturemate.culturemate_api.dto.BoardSearchDto;
import com.culturemate.culturemate_api.dto.ImageUploadRequestDto;
import com.culturemate.culturemate_api.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Board API", description = "게시판 관리 API")
@RestController
@RequestMapping("/api/v1/board")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class BoardController {

  private final BoardService boardService;

  @Operation(summary = "전체 게시물 조회", description = "모든 게시물을 조회합니다")
  @GetMapping
  public ResponseEntity<List<BoardDto.Response>> getAllBoards() {
    return ResponseEntity.ok().body(
      boardService.findAll().stream()
        .map(BoardDto.Response::from)
        .collect(Collectors.toList()));
  }

  @Operation(summary = "특정 게시물 조회", description = "ID로 특정 게시물을 조회합니다")
  @GetMapping("/{boardId}")
  public ResponseEntity<BoardDto.Response> getBoard(@Parameter(description = "게시물 ID", required = true) @PathVariable Long boardId) {
    return ResponseEntity.ok().body(
      BoardDto.Response.from(boardService.findById(boardId))
    );
  }

  // 작성자 기준 게시글 조회
  @GetMapping("/author/{memberId}")
  public ResponseEntity<List<BoardDto.Response>> getByAuthor(@PathVariable Long memberId) {
    return ResponseEntity.ok(
      boardService.findByAuthor(memberId).stream()
        .map(BoardDto.Response::from)
        .collect(Collectors.toList())
    );
  }

  // 통합 검색
  @GetMapping("/search")
  public ResponseEntity<List<BoardDto.Response>> searchBoards(BoardSearchDto searchDto) {
    if (searchDto.isEmpty()) {
      throw new IllegalArgumentException("검색 조건을 하나 이상 입력해주세요.");
    }
    
    List<BoardDto.Response> boards = boardService.search(searchDto).stream()
      .map(BoardDto.Response::from)
      .collect(Collectors.toList());
    return ResponseEntity.ok(boards);
  }

  @Operation(summary = "게시글 생성", description = "새로운 게시글을 작성합니다")
  @PostMapping
  public ResponseEntity<BoardDto.Response> createBoard(@Parameter(description = "게시글 작성 정보", required = true)
                                                         @Valid @RequestBody BoardDto.Request requestDto) {
    return ResponseEntity.ok(
      BoardDto.Response.from(boardService.create(requestDto))
    );
  }

  // 게시글 수정
  @PutMapping("/{boardId}")
  public ResponseEntity<BoardDto.Response> updateBoard(@PathVariable Long boardId,
                                                      @Valid @RequestBody BoardDto.Request requestDto,
                                                      @AuthenticationPrincipal AuthenticatedUser requester) {
    return ResponseEntity.ok(
      BoardDto.Response.from(boardService.update(boardId, requestDto, requester.getMemberId()))
    );
  }

  // 게시글 삭제
  @DeleteMapping("/{boardId}")
  public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId,
                                         @AuthenticationPrincipal AuthenticatedUser requester) {
    boardService.delete(boardId, requester.getMemberId());
    return ResponseEntity.noContent().build();
  }

  // 좋아요 토글 (추가/취소)
  @PostMapping("/{boardId}/like")
  public ResponseEntity<String> toggleBoardLike(@PathVariable Long boardId,
                                                @RequestParam Long memberId) {
    boolean liked = boardService.toggleBoardLike(boardId, memberId);

    if (liked) {
      return ResponseEntity.ok("좋아요 성공");
    } else {
      return ResponseEntity.ok("좋아요 취소");
    }
  }
}
