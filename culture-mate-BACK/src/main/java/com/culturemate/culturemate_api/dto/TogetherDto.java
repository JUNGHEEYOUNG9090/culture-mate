package com.culturemate.culturemate_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TogetherDto {

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "TogetherRequest")
  public static class Request {

    @NotNull(message = "이벤트 ID는 필수입니다.")
    private Long eventId;
    
    @NotNull(message = "호스트 ID는 필수입니다.")
    private Long hostId;
    
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;
    
    @NotNull(message = "지역 정보는 필수입니다.")
    private RegionDto.Request region;
    
    @NotBlank(message = "모임장소는 필수입니다.")
    @Size(max = 255, message = "모임장소는 255자를 초과할 수 없습니다.")
    private String meetingLocation;
    
    @NotNull(message = "모임 날짜는 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate meetingDate;
    
    @NotNull(message = "최대 참여자 수는 필수입니다.")
    @Min(value = 2, message = "최대 참여자 수는 2명 이상이어야 합니다.")
    @Max(value = 100, message = "최대 참여자 수는 100명을 초과할 수 없습니다.")
    private Integer maxParticipants;
    
    @Size(max = 2000, message = "내용은 2000자를 초과할 수 없습니다.")
    private String content;

  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "TogetherResponse", description = "모임 상세 정보 응답 DTO")
  public static class Response {

    private Long id;
    private EventDto.ResponseCard event;
    private MemberDto.ProfileResponse host;
    private String title;
    private String content;
    private RegionDto.Response region;
    private String meetingLocation;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate meetingDate;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer interestCount; // 관심수
    private Boolean active;
    private Boolean isInterested; // 관심 등록 여부
    private Long roomId; // 채팅방 ID (Together의 채팅방)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

  }
}