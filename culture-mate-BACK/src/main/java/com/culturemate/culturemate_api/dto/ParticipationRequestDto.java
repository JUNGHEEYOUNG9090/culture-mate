package com.culturemate.culturemate_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ParticipationRequest", description = "동행 참여 신청 정보")
public class ParticipationRequestDto {

  @Schema(description = "신청 ID", example = "12345")
  private Long requestId;

  @Schema(description = "동행 ID", example = "1")
  private Long togetherId;

  @Schema(description = "동행 제목", example = "한강 벚꽃 구경 같이 가요")
  private String togetherTitle;

  @Schema(description = "신청자 ID", example = "2")
  private Long applicantId;

  @Schema(description = "신청자 이름", example = "홍길동")
  private String applicantName;

  @Schema(description = "신청자 프로필 이미지", example = "/images/profile/123.jpg")
  private String applicantProfileImage;

  @Schema(description = "호스트 ID", example = "1")
  private Long hostId;

  @Schema(description = "호스트 이름", example = "김호스트")
  private String hostName;

  @Schema(description = "신청 상태", example = "PENDING")
  private String status;

  @Schema(description = "신청 메시지", example = "함께 참여하고 싶습니다!")
  private String message;

  @Schema(description = "이벤트 이름", example = "서울 벚꽃축제")
  private String eventName;

  @Schema(description = "이벤트 타입", example = "축제")
  private String eventType;

  @Schema(description = "이벤트 이미지", example = "/images/event/festival.jpg")
  private String eventImage;

  @Schema(description = "모임 날짜", example = "2024-04-15")
  private LocalDate meetingDate;

  @Schema(description = "신청 일시", example = "2024-03-20T10:30:00")
  private LocalDateTime createdAt;

  @Schema(description = "신청용 채팅방 ID", example = "12345")
  private Long applicationChatRoomId;

  @Schema(description = "신청용 채팅방 이름", example = "[한강 벚꽃 구경] 신청 문의")
  private String applicationChatRoomName;
}