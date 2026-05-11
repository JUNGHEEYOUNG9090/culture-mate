package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.domain.event.EventReview;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.dto.ReviewDto;
import com.culturemate.culturemate_api.service.EventReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Event Review API", description = "이벤트 리뷰 관리 API")
@RestController
@RequestMapping("/api/v1/event-reviews")
@RequiredArgsConstructor
public class EventReviewController {
  private final EventReviewService eventReviewService;

  // 특정 이벤트의 리뷰 데이터 조회
  @Operation(summary = "특정 이벤트의 리뷰 조회", description = "특정 이벤트의 아이디를 전달하면 해당 이벤트의 리뷰를 반환")
  @GetMapping("/{eventId}")
  public ResponseEntity<List<ReviewDto.Response>> getEventReviews(@PathVariable Long eventId) {
    return ResponseEntity.ok(
      eventReviewService.findByEventId(eventId).stream()
        .map(ReviewDto.Response::from)
        .collect(Collectors.toList())
    );
  }

  // 사용자의 리뷰 데이터 조회
  @Operation(summary = "요청자의 리뷰데이터 조회", description = "요청자 정보를 기반으로 작성한 리뷰 데이터를 이벤트 정보화 함께 반환")
  @GetMapping("/my")
  public ResponseEntity<List<ReviewDto.ResponseWithEvent>> getMyEventReviews(@AuthenticationPrincipal AuthenticatedUser requester) {
    return ResponseEntity.ok(
      eventReviewService.findByMemberId(requester.getMemberId()).stream()
        .map(ReviewDto.ResponseWithEvent::from)
        .collect(Collectors.toList())
    );
  }

  // 이벤트 리뷰 등록
  @Operation(summary = "이벤트 리뷰 등록", description = "이벤트 리뷰를 등록합니다.")
  @PostMapping
  public ResponseEntity<ReviewDto.Response> createEventReview(
      @Valid @RequestBody ReviewDto.Request reviewDto,
      @AuthenticationPrincipal AuthenticatedUser requester) {
    EventReview createdReview = eventReviewService.create(reviewDto, requester.getMemberId());
    return ResponseEntity.status(201).body(ReviewDto.Response.from(createdReview));
  }

  // 이벤트 리뷰 수정
  @Operation(summary = "이벤트 리뷰 수정", description = "이벤트 리뷰를 수정합니다.(작성자 본인만)")
  @PutMapping("/{id}")
  public ResponseEntity<ReviewDto.Response> updateEventReview(
      @PathVariable Long id, 
      @Valid @RequestBody ReviewDto.Request reviewDto,
      @AuthenticationPrincipal AuthenticatedUser requester) {
    EventReview updatedEventReview = eventReviewService.update(id, reviewDto, requester.getMemberId());
    return ResponseEntity.ok(ReviewDto.Response.from(updatedEventReview));
  }

  // 이벤트 리뷰 ID로 삭제
  @Operation(summary = "이벤트 리뷰 삭제", description = "이벤트 리뷰를 삭제합니다.(작성자 본인만)")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEventReview(
      @PathVariable Long id,
      @AuthenticationPrincipal AuthenticatedUser requester) {
    eventReviewService.delete(id, requester.getMemberId());
    return ResponseEntity.noContent().build();
  }
}