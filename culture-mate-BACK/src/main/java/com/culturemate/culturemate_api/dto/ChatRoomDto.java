package com.culturemate.culturemate_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.culturemate.culturemate_api.domain.chatting.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class ChatRoomDto {

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "ChatRoomResponse", description = "채팅방 정보 응답 DTO")
  public static class Response {
    private Long id;
    private String roomName;
    private Integer chatMemberCount;
    private LocalDateTime createdAt;

    public static Response from(ChatRoom chatRoom) {
      return Response.builder()
        .id(chatRoom.getId())
        .roomName(chatRoom.getRoomName())
        .chatMemberCount(chatRoom.getChatMemberCount())
        .createdAt(chatRoom.getCreatedAt() != null ? 
                   chatRoom.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
        .build();
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "ChatRoomResponseDetail")
  public static class ResponseDetail {
    private Long id;
    private String roomName;
    private Integer chatMemberCount;
    private MemberDto.ProfileResponse host;
    private List<MemberDto.ProfileResponse> participants;
    private LocalDateTime createdAt;

    public static ResponseDetail from(ChatRoom chatRoom) {
      List<MemberDto.ProfileResponse> participants = chatRoom.getChatMembers().stream()
        .map(chatMember -> MemberDto.ProfileResponse.from(chatMember.getMember()))
        .collect(Collectors.toList());

      return ResponseDetail.builder()
        .id(chatRoom.getId())
        .roomName(chatRoom.getRoomName())
        .chatMemberCount(chatRoom.getChatMemberCount())
        .host(MemberDto.ProfileResponse.from(chatRoom.getTogether().getHost()))
        .participants(participants)
        .createdAt(chatRoom.getCreatedAt() != null ? 
                   chatRoom.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
        .build();
    }
  }
}