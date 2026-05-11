package com.culturemate.culturemate_api.dto;

import com.culturemate.culturemate_api.domain.chatting.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;          // 메시지 ID
    private Long roomId;
    private Long senderId;    // 메시지 작성자 ID
    private String content;
    private LocalDateTime createdAt;  // 생성 시간

    public static ChatMessageDto from(ChatMessage chatMessage) {
        return ChatMessageDto.builder()
            .id(chatMessage.getId())
            .roomId(chatMessage.getChatRoom().getId())
            .senderId(chatMessage.getAuthor().getMember().getId())
            .content(chatMessage.getContent())
            .createdAt(chatMessage.getCreatedAt() != null ? 
                       chatMessage.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
            .build();
    }
}
