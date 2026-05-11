package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import com.culturemate.culturemate_api.domain.chatting.ChatMessage;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.dto.ChatMessageDto;
import com.culturemate.culturemate_api.dto.ChatRoomDto;
import com.culturemate.culturemate_api.service.ChatRoomService;
import com.culturemate.culturemate_api.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "ChatRoom API", description = "채팅방 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatroom")
public class ChatRoomController {

  private final ChatRoomService chatRoomService;
  private final MemberService memberService;

  // 모든 채팅방 조회 (관리자용)
  @GetMapping
  public ResponseEntity<List<ChatRoomDto.Response>> getAllChatRooms() {
    List<ChatRoom> chatRooms = chatRoomService.findAllRoom();
    List<ChatRoomDto.Response> response = chatRooms.stream()
      .map(ChatRoomDto.Response::from)
      .collect(Collectors.toList());
    return ResponseEntity.ok(response);
  }

  // 채팅방 생성
  @PostMapping("/create")
  public ResponseEntity<ChatRoomDto.Response> createChatRoom() {
    ChatRoom chatRoom = chatRoomService.createChatRoom();
    return ResponseEntity.ok(ChatRoomDto.Response.from(chatRoom));
  }

  // 특정 사용자의 채팅방 불러오기 (로그인한 회원)
  @GetMapping("/my")
  public ResponseEntity<List<ChatRoomDto.Response>> getMyChatRooms(@AuthenticationPrincipal AuthenticatedUser requester) {
    Member member = memberService.findByLoginId(requester.getUsername());
    List<ChatRoom> chatRooms = chatRoomService.findRoomsByMember(member.getId());
    List<ChatRoomDto.Response> response = chatRooms.stream()
        .map(ChatRoomDto.Response::from)
        .collect(Collectors.toList());
    return ResponseEntity.ok(response);
  }

  // 채팅방 입장 (새로운 채팅방)
  @PostMapping("/{roomId}/join")
  public ResponseEntity<Void> joinChatRoom(@PathVariable Long roomId,
                                           @AuthenticationPrincipal AuthenticatedUser requester) {
    Member member = memberService.findByLoginId(requester.getUsername());
    chatRoomService.addMemberToRoom(roomId, member.getId());
    return ResponseEntity.noContent().build();
  }

  // 채팅방 접속 (내가 참여중인 채팅방)
  @GetMapping("/{roomId}")
  @Operation(summary = "채팅방 상세 정보 조회", description = "참여 중인 채팅방의 상세 정보를 조회합니다.")
  public ResponseEntity<ChatRoomDto.ResponseDetail> getChatRoom(
      @PathVariable Long roomId,
      @AuthenticationPrincipal AuthenticatedUser user) {

    Member member = memberService.findByLoginId(user.getUsername());

    // 권한 검증: 채팅방 접근 권한 확인
    if (!chatRoomService.canAccessChatRoom(roomId, member.getId())) {
      throw new SecurityException("해당 채팅방에 접근 권한이 없습니다.");
    }

    ChatRoom chatRoom = chatRoomService.findById(roomId);
    return ResponseEntity.ok(ChatRoomDto.ResponseDetail.from(chatRoom));
  }

  // 이전 대화내역 불러오기
  @GetMapping("/{roomId}/messages")
  @Operation(summary = "채팅 메시지 페이징 조회", description = "참여 중인 채팅방의 메시지 목록을 페이징하여 조회합니다. `?page=0&size=20&sort=createdAt,desc`와 같이 사용합니다.")
  public ResponseEntity<Page<ChatMessageDto>> getChatMessages(
      @PathVariable Long roomId,
      @AuthenticationPrincipal AuthenticatedUser user,
      @Parameter(hidden = true) Pageable pageable) {

    Member member = memberService.findByLoginId(user.getUsername());

    // 권한 검증: 채팅방 접근 권한 확인
    if (!chatRoomService.canAccessChatRoom(roomId, member.getId())) {
      throw new SecurityException("해당 채팅방에 접근 권한이 없습니다.");
    }

    Page<ChatMessage> messagesPage = chatRoomService.getMessagesByRoomId(roomId, pageable);
    Page<ChatMessageDto> messageDtosPage = messagesPage.map(ChatMessageDto::from);
    
    return ResponseEntity.ok(messageDtosPage);
  }

  // 채팅방 나가기
  @DeleteMapping("/{roomId}/leave")
  public ResponseEntity<Void> leaveChatRoom(@PathVariable Long roomId,
                                            @AuthenticationPrincipal AuthenticatedUser requester) {
    Member member = memberService.findByLoginId(requester.getUsername());
    chatRoomService.removeMemberFromRoom(roomId, member.getId());
    return ResponseEntity.noContent().build();
  }

}
