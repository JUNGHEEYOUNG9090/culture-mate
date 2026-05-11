package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.domain.chatting.ChatMessage;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.dto.ChatMessageDto;
import com.culturemate.culturemate_api.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

  private final ChatRoomService chatRoomService;
  private final SimpMessagingTemplate messagingTemplate;


  // 메시지 전송 엔드포인트
  @MessageMapping("/chatroom/{roomId}/send")
  public void sendMessage(@DestinationVariable Long roomId,
                                ChatMessageDto messageDto,
                                Principal principal) {

    log.debug("채팅 메시지 수신: roomId={}", roomId);

    // Principal에서 AuthenticatedUser 추출
    AuthenticatedUser user = null;
    if (principal instanceof UsernamePasswordAuthenticationToken) {
      UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
      if (auth.getPrincipal() instanceof AuthenticatedUser) {
        user = (AuthenticatedUser) auth.getPrincipal();
      }
    }

    try {
      // 1. DTO 검증
      if (messageDto == null) {
        throw new IllegalArgumentException("메시지 데이터가 없습니다");
      }

      if (messageDto.getContent() == null || messageDto.getContent().trim().isEmpty()) {
        throw new IllegalArgumentException("메시지 내용이 필요합니다");
      }

      // 2. 사용자 인증 확인
      if (user == null) {
        log.warn("인증되지 않은 사용자의 채팅 시도: roomId={}", roomId);
        throw new IllegalArgumentException("인증이 필요합니다");
      }

      Long senderId = user.getMemberId();

      // 3. roomId 일치 검증 (path variable vs DTO)
      if (messageDto.getRoomId() != null && !messageDto.getRoomId().equals(roomId)) {
        log.debug("roomId 불일치, path variable 사용: path={}, dto={}", roomId, messageDto.getRoomId());
      }

      // 4. 채팅방 참여 권한 검증 및 메시지 저장 (서비스에서 처리)
      ChatMessage savedMessage = chatRoomService.sendMessage(
          roomId, senderId, messageDto.getContent().trim()
      );

      log.info("채팅 메시지 저장 성공: roomId={}, messageId={}, senderId={}",
          roomId, savedMessage.getId(), senderId);

      // 5. 채팅방 구독자들에게 메시지 전송
      ChatMessageDto responseDto = ChatMessageDto.from(savedMessage);
      messagingTemplate.convertAndSend(
        "/topic/chatroom/" + roomId,
        responseDto
      );

      log.debug("채팅 메시지 브로드캐스트 완료: roomId={}", roomId);

    } catch (Exception e) {
      log.error("채팅 메시지 처리 실패: roomId={}, error={}", roomId, e.getMessage());
      throw e;
    }
  }

  // 채팅방 입장 알림
  @MessageMapping("/chatroom/{roomId}/join")
  public void joinRoom(@DestinationVariable Long roomId,
                      @AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null) {
      log.warn("인증되지 않은 사용자의 채팅방 입장 시도: roomId={}", roomId);
      throw new IllegalArgumentException("인증이 필요합니다");
    }

    try {
      // 권한 검증 및 멤버 추가
      chatRoomService.checkAndAddMemberToRoom(roomId, user.getMemberId());

      // 입장 알림
      messagingTemplate.convertAndSend("/topic/chatroom/" + roomId,
          Map.of("type", "JOIN", "message", user.getNickname() + "님이 입장했습니다"));

      log.info("채팅방 입장 완료: roomId={}, user={}", roomId, user.getLoginId());
    } catch (Exception e) {
      log.error("채팅방 입장 실패: roomId={}, user={}, error={}",
          roomId, user.getLoginId(), e.getMessage());
      throw e;
    }
  }

}