package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.event.Event;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.together.Participants;
import com.culturemate.culturemate_api.domain.together.ParticipationStatus;
import com.culturemate.culturemate_api.domain.together.Together;
import com.culturemate.culturemate_api.dto.*;
import com.culturemate.culturemate_api.repository.ParticipantsRepository;
import com.culturemate.culturemate_api.facade.TogetherFacade;
import com.culturemate.culturemate_api.service.ChatRoomService;
import com.culturemate.culturemate_api.service.MemberService;
import com.culturemate.culturemate_api.service.TogetherService;
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

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Together API", description = "그룹 모임 관리 API")
@RestController
@RequestMapping("/api/v1/together")
@RequiredArgsConstructor
public class TogetherController {
  private final TogetherService togetherService;
  private final TogetherFacade togetherFacade;
  private final MemberService memberService;
  private final ChatRoomService chatRoomService;
  private final ParticipantsRepository participantsRepository;

  @Operation(summary = "전체 모임 조회", description = "모든 모임 목록을 조회합니다")
  @GetMapping
  public ResponseEntity<List<TogetherDto.Response>> getAllTogethers(
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer offset,
      @AuthenticationPrincipal AuthenticatedUser user) {
    List<Together> togethers;
    if (limit != null) {
      togethers = togetherService.findAll(limit, offset != null ? offset : 0);
    } else {
      togethers = togetherService.findAll();
    }

    if (user != null) {
      // 인증된 사용자: 관심 여부 포함
      List<Long> togetherIds = togethers.stream().map(Together::getId).toList();
      Map<Long, Boolean> interestMap = togetherService.getInterestStatusBatch(togetherIds, user.getMemberId());

      List<TogetherDto.Response> responseDtos = togethers.stream()
        .map(together -> togetherService.toResponseDto(together, interestMap.getOrDefault(together.getId(), false)))
        .collect(Collectors.toList());
      return ResponseEntity.ok(responseDtos);
    } else {
      // 비인증 사용자: 관심 여부 미포함
      return ResponseEntity.ok().body(
        togethers.stream()
          .map(togetherService::toResponseDto)
          .collect(Collectors.toList())
      );
    }
  }

  @Operation(summary = "특정 모임 조회", description = "ID로 특정 모임을 조회합니다")
  @GetMapping("/{id}")
  public ResponseEntity<TogetherDto.Response> getTogetherById(@Parameter(description = "모임 ID", required = true) @PathVariable Long id,
                                                              @AuthenticationPrincipal AuthenticatedUser user) {
    Together together = togetherService.findById(id);

    boolean isInterested = false;
    if (user != null) {
      isInterested = togetherService.isInterested(id, user.getMemberId());
    }

    return ResponseEntity.ok().body(togetherService.toResponseDto(together, isInterested));
  }

  @Operation(summary = "호스트별 모임 조회", description = "특정 회원이 호스트인 모임을 조회합니다")
  @GetMapping("/hosted-by/{hostId}")
  public ResponseEntity<List<TogetherDto.Response>> getTogethersByHostId(@Parameter(description = "호스트 회원 ID", required = true)
                                                                           @PathVariable Long hostId) {
    Member host = memberService.findById(hostId);
    List<Together> togethers = togetherService.findByHost(host);
    return ResponseEntity.ok().body(
      togethers.stream()
      .map(togetherService::toResponseDto)
      .collect(Collectors.toList()));
  }

  // 특정 회원이 실제 참여 중인 모집글 조회 (승인된 것만)
  @GetMapping("/with/{memberId}")
  public ResponseEntity<List<TogetherDto.Response>> getTogethersByMemberId(@PathVariable Long memberId) {
    Member member = memberService.findById(memberId);
    List<Together> togethers = togetherService.findByMemberAndStatus(member, "APPROVED");
    return ResponseEntity.ok().body(
      togethers.stream()
      .map(togetherService::toResponseDto)
      .collect(Collectors.toList()));
  }

  // 모집글 통합 검색
  @GetMapping("/search")
  public ResponseEntity<List<TogetherDto.Response>> searchTogethers(
      TogetherSearchDto searchDto,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer offset,
      @RequestParam(required = false, defaultValue = "latest") String sortBy,
      @AuthenticationPrincipal AuthenticatedUser user) {

    SearchResult<Together> searchResult;
    // 심플한 통합 호출 - Service에서 limit 기반 분기 처리
    if (searchDto.isEmpty()) {
      // 전체 조회인 경우에도 SearchResult로 래핑 필요
      List<Together> togethers = togetherService.findAll(limit, offset != null ? offset : 0, sortBy);
      searchResult = new SearchResult<>(togethers, togethers.size()); // 임시로 전체 개수는 결과 개수와 동일
    } else {
      searchResult = togetherService.search(searchDto, limit, offset != null ? offset : 0, sortBy);
    }

    List<TogetherDto.Response> responseDtos;
    if (user != null) {
      // 인증된 사용자: 관심 여부 포함
      List<Long> togetherIds = searchResult.getContent().stream().map(Together::getId).toList();
      Map<Long, Boolean> interestMap = togetherService.getInterestStatusBatch(togetherIds, user.getMemberId());

      responseDtos = searchResult.getContent().stream()
        .map(together -> togetherService.toResponseDto(together, interestMap.getOrDefault(together.getId(), false)))
        .collect(Collectors.toList());
    } else {
      // 비인증 사용자: 기본값 false
      responseDtos = searchResult.getContent().stream()
        .map(togetherService::toResponseDto)
        .collect(Collectors.toList());
    }

    return ResponseEntity.ok()
      .header("Total-Count", String.valueOf(searchResult.getTotalCount()))
      .body(responseDtos);
  }

  // 모집글 생성
  @PostMapping
  public ResponseEntity<TogetherDto.Response> createTogether(@Valid @RequestBody TogetherDto.Request togetherRequestDto) {
    Together together = togetherFacade.createTogetherWithChatRoom(togetherRequestDto);
    return ResponseEntity.ok().body(togetherService.toResponseDto(together));
  }

  // 모집글 수정
  @PutMapping("/{togetherId}")
  public ResponseEntity<TogetherDto.Response> updateTogether(@PathVariable Long togetherId,
                                                             @Valid @RequestBody TogetherDto.Request togetherRequestDto,
                                                             @AuthenticationPrincipal AuthenticatedUser requester) {
    Together updatedTogether = togetherService.update(togetherId, togetherRequestDto, requester.getMemberId());
    return ResponseEntity.ok().body(togetherService.toResponseDto(updatedTogether));
  }

  // 모집글 삭제
  @DeleteMapping("/{togetherId}")
  public ResponseEntity<Void> deleteTogether(@PathVariable Long togetherId,
                                            @AuthenticationPrincipal AuthenticatedUser requester) {
    togetherFacade.deleteTogetherWithRelatedData(togetherId, requester.getMemberId());
    return ResponseEntity.noContent().build();
  }

  // 모집글의 채팅방 조회
  @GetMapping("/{togetherId}/chatroom")
  @Operation(summary = "모집글 채팅방 조회", description = "해당 모집글의 채팅방 정보를 조회합니다. 호스트이거나 승인된 참여자만 접근 가능합니다.")
  public ResponseEntity<ChatRoomDto.ResponseDetail> getTogetherChatRoom(@PathVariable Long togetherId,
                                                                        @AuthenticationPrincipal AuthenticatedUser requester) {
    Together together = togetherService.findById(togetherId);
    ChatRoom chatRoom = chatRoomService.findGroupChatByTogether(together);

    // 권한 검증: 호스트이거나 승인된 참여자만 채팅방 정보 접근 가능
    if (!chatRoomService.canAccessChatRoom(chatRoom.getId(), requester.getMemberId())) {
      throw new SecurityException("해당 채팅방에 접근 권한이 없습니다. 동행 호스트이거나 승인된 참여자만 접근할 수 있습니다.");
    }

    return ResponseEntity.ok(ChatRoomDto.ResponseDetail.from(chatRoom));
  }

  // 동행 신청 (승인 대기) - 신청용 1:1 채팅방 자동 생성
  @PostMapping("/{togetherId}/apply")
  public ResponseEntity<Void> applyTogether(@PathVariable Long togetherId,
                                            @RequestParam String message,
                                            @AuthenticationPrincipal AuthenticatedUser requester) {
    // 동행 신청 및 신청용 채팅방 생성을 통합 처리
    togetherService.applyTogether(togetherId, requester.getMemberId(), message);

    return ResponseEntity.ok().build();
  }

  // 동행 참여 승인
  @PostMapping("/{togetherId}/participants/{participantId}/approve")
  public ResponseEntity<Void> approveParticipation(@PathVariable Long togetherId,
                                                   @PathVariable Long participantId,
                                                   @AuthenticationPrincipal AuthenticatedUser requester) {
    togetherFacade.approveParticipationWithChatRoom(togetherId, participantId, requester.getMemberId());
    return ResponseEntity.ok().build();
  }

  // 동행 참여 거절
  @PostMapping("/{togetherId}/participants/{participantId}/reject")
  public ResponseEntity<Void> rejectParticipation(@PathVariable Long togetherId,
                                                  @PathVariable Long participantId,
                                                  @AuthenticationPrincipal AuthenticatedUser requester) {
    togetherFacade.rejectParticipation(togetherId, participantId, requester.getMemberId());
    return ResponseEntity.ok().build();
  }

  // 참여자 목록 조회 (상태별 필터링 가능)
  @GetMapping("/{togetherId}/participants")
  public ResponseEntity<List<MemberDto.Response>> getTogetherParticipants(@PathVariable Long togetherId,
                                                                          @RequestParam(required = false) String status) {
    List<Member> participants;
    if (status == null) {
      participants = togetherService.getAllParticipants(togetherId);
    } else {
      participants = togetherService.getParticipantsByStatus(togetherId, status);
    }
    
    List<MemberDto.Response> participantDtos = participants.stream()
      .map(MemberDto.Response::from)
      .collect(Collectors.toList());
    
    return ResponseEntity.ok().body(participantDtos);
  }

  // 참여 취소 (본인)
  @DeleteMapping("/{togetherId}/participants/cancel")
  public ResponseEntity<Void> cancelParticipation(@PathVariable Long togetherId,
                                                  @AuthenticationPrincipal AuthenticatedUser requester) {
    togetherService.leaveTogether(togetherId, requester.getMemberId());
    return ResponseEntity.ok().build();
  }

  // 호스트의 참여자 강제 퇴출
  @DeleteMapping("/{togetherId}/participants/{participantId}")
  public ResponseEntity<Void> removeTogetherMember(@PathVariable Long togetherId,
                                                   @PathVariable Long participantId,
                                                   @AuthenticationPrincipal AuthenticatedUser requester) {
    togetherService.removeMember(togetherId, participantId, requester.getMemberId());
    return ResponseEntity.ok().build();
  }

  // 호스트 모집상태 변경
  @PatchMapping("/{togetherId}/recruiting/{status}")
  public ResponseEntity<Void> changeRecruitingStatus(@PathVariable Long togetherId,
                                                     @PathVariable String status,
                                                     @AuthenticationPrincipal AuthenticatedUser requester) {
    if ("close".equals(status)) {
      togetherService.closeTogether(togetherId, requester.getMemberId());
    } else if ("reopen".equals(status)) {
      togetherService.reopenTogether(togetherId, requester.getMemberId());
    } else {
      throw new IllegalArgumentException("상태는 'close' 또는 'reopen'이어야 합니다.");
    }
    return ResponseEntity.ok().build();
  }

  // 내 신청 목록 조회 (상태별 필터링 가능)
  @GetMapping("/my-applications")
  @Operation(summary = "보낸 신청 목록 조회", description = "내가 참여 신청한 동행 목록을 조회합니다")
  public ResponseEntity<List<ParticipationRequestDto>> getMyApplications(
      @RequestParam(required = false) String status,
      @AuthenticationPrincipal AuthenticatedUser requester) {

    // 내가 신청한 동행들의 실제 참여 신청 데이터 조회
    List<ParticipationRequestDto> myApplications = participantsRepository
        .findByParticipant_IdAndStatusInOrderByCreatedAtDesc(
            requester.getMemberId(),
            status != null ? List.of(ParticipationStatus.valueOf(status.toUpperCase()))
                          : List.of(ParticipationStatus.PENDING, ParticipationStatus.APPROVED, ParticipationStatus.REJECTED, ParticipationStatus.CANCELED)
        )
        .stream()
        .filter(participant -> !participant.getParticipant().getId().equals(participant.getTogether().getHost().getId())) // 내가 호스트인 경우 제외
        .map(participant -> {
          Together together = participant.getTogether();
          Member applicant = participant.getParticipant(); // 신청자 (나)
          Member host = together.getHost(); // 호스트

          return ParticipationRequestDto.builder()
              .requestId(participant.getId())
              .togetherId(together.getId())
              .togetherTitle(together.getTitle())
              .applicantId(applicant.getId())
              .applicantName(applicant.getMemberDetail() != null ? applicant.getMemberDetail().getNickname() : applicant.getLoginId())
              .applicantProfileImage(applicant.getMemberDetail() != null ? applicant.getMemberDetail().getThumbnailImagePath() : null)
              .hostId(host.getId())
              .hostName(host.getMemberDetail() != null ? host.getMemberDetail().getNickname() : host.getLoginId())
              .status(participant.getStatus().name())
              .message(participant.getMessage() != null ? participant.getMessage() : "동행 신청 메시지")
              .eventName(together.getEvent().getTitle())
              .eventType(together.getEvent().getEventType().name())
              .eventImage(together.getEvent().getThumbnailImagePath())
              .meetingDate(together.getMeetingDate())
              .createdAt(participant.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
              .applicationChatRoomId(participant.getApplicationChatRoom() != null ?
                                    participant.getApplicationChatRoom().getId() : null)
              .applicationChatRoomName(participant.getApplicationChatRoom() != null ?
                                      participant.getApplicationChatRoom().getRoomName() : null)
              .build();
        })
        .collect(Collectors.toList());

    return ResponseEntity.ok(myApplications);
  }

  // 관심 등록/해제
  @Operation(summary = "동행 관심 등록/해제", description = "특정 동행에 대한 관심을 등록하거나 해제합니다")
  @PostMapping("/{togetherId}/interest")
  public ResponseEntity<String> toggleTogetherInterest(@Parameter(description = "동행 ID", required = true) @PathVariable Long togetherId,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null) {
      return ResponseEntity.status(401).body("인증이 필요합니다");
    }

    boolean interest = togetherService.toggleInterest(togetherId, user.getMemberId());

    if (interest) {
      return ResponseEntity.ok("관심 등록");
    } else {
      return ResponseEntity.ok("관심 취소");
    }
  }

  // 내가 관심 등록한 동행 목록 조회
  @Operation(summary = "관심 동행 목록 조회", description = "사용자가 관심 등록한 동행 목록을 조회합니다")
  @GetMapping("/my-interests")
  public ResponseEntity<List<TogetherDto.Response>> getMyInterests(@AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null) {
      return ResponseEntity.status(401).build();
    }

    List<Together> interestTogethers = togetherService.getUserInterestTogethers(user.getMemberId());
    List<TogetherDto.Response> responseDtos = interestTogethers.stream()
      .map(togetherService::toResponseDto)
      .collect(Collectors.toList());

    return ResponseEntity.ok(responseDtos);
  }

  // 받은 신청 목록 조회 (내가 호스트인 동행의 참여 신청 목록)
  @GetMapping("/received-applications")
  @Operation(summary = "받은 신청 목록 조회", description = "내가 호스트인 동행들에 대한 참여 신청 목록을 조회합니다")
  public ResponseEntity<List<ParticipationRequestDto>> getReceivedApplications(
      @RequestParam(required = false) String status,
      @AuthenticationPrincipal AuthenticatedUser requester) {

    // 내가 호스트인 동행들의 실제 참여 신청 데이터 조회
    List<ParticipationRequestDto> receivedApplications = participantsRepository
        .findByTogether_HostIdAndStatusInOrderByCreatedAtDesc(
            requester.getMemberId(),
            status != null ? List.of(ParticipationStatus.valueOf(status.toUpperCase()))
                          : List.of(ParticipationStatus.PENDING, ParticipationStatus.APPROVED, ParticipationStatus.REJECTED, ParticipationStatus.CANCELED)
        )
        .stream()
        .filter(participant -> !participant.getParticipant().getId().equals(participant.getTogether().getHost().getId())) // 호스트 제외
        .map(participant -> {
          Together together = participant.getTogether();
          Member applicant = participant.getParticipant();
          Event event = together.getEvent();

          return ParticipationRequestDto.builder()
              .requestId(participant.getId())
              .togetherId(together.getId())
              .togetherTitle(together.getTitle())
              .applicantId(applicant.getId())
              .applicantName(applicant.getMemberDetail() != null ?
                  applicant.getMemberDetail().getNickname() : applicant.getLoginId())
              .applicantProfileImage(applicant.getMemberDetail() != null ?
                  applicant.getMemberDetail().getThumbnailImagePath() : null)
              .hostId(together.getHost().getId())
              .hostName(together.getHost().getMemberDetail() != null ?
                  together.getHost().getMemberDetail().getNickname() : together.getHost().getLoginId())
              .status(participant.getStatus().name())
              .message(participant.getMessage())
              .eventName(event != null ? event.getTitle() : "")
              .eventType(event != null ? event.getEventType().name() : "")
              .eventImage(event != null ? event.getThumbnailImagePath() : null)
              .meetingDate(together.getMeetingDate())
              .createdAt(participant.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
              // 채팅방 정보 추가
              .applicationChatRoomId(participant.getApplicationChatRoom() != null ?
                  participant.getApplicationChatRoom().getId() : null)
              .applicationChatRoomName(participant.getApplicationChatRoom() != null ?
                  participant.getApplicationChatRoom().getRoomName() : null)
              .build();
        })
        .collect(Collectors.toList());

    return ResponseEntity.ok(receivedApplications);
  }

  // 임시 디버깅 엔드포인트 - 실제 Participants 데이터 확인 (개발 환경 전용)
  @GetMapping("/debug/participants")
  @org.springframework.context.annotation.Profile("!prod") // 프로덕션 환경에서는 비활성화
  public ResponseEntity<String> debugParticipants(@AuthenticationPrincipal AuthenticatedUser requester) {
    List<Participants> allParticipants = participantsRepository.findAll();

    StringBuilder debug = new StringBuilder();
    debug.append("전체 참여 데이터 개수: ").append(allParticipants.size()).append("\n\n");

    for (Participants p : allParticipants) {
      debug.append("참여자 ID: ").append(p.getId())
           .append(", 동행 ID: ").append(p.getTogether() != null ? p.getTogether().getId() : "null")
           .append(", 호스트 ID: ").append(p.getTogether() != null && p.getTogether().getHost() != null ? p.getTogether().getHost().getId() : "null")
           .append(", 신청자 ID: ").append(p.getParticipant() != null ? p.getParticipant().getId() : "null")
           .append(", 상태: ").append(p.getStatus())
           .append(", 생성일: ").append(p.getCreatedAt())
           .append(", 메시지: ").append(p.getMessage())
           .append(", 채팅방 ID: ").append(p.getApplicationChatRoom() != null ? p.getApplicationChatRoom().getId() : "null")
           .append("\n");
    }

    debug.append("\n현재 사용자 ID: ").append(requester.getMemberId()).append("\n");

    return ResponseEntity.ok(debug.toString());
  }

  // /recent 엔드포인트 제거됨 - 대신 /search?limit=4&sortBy=latest 사용 권장

}
