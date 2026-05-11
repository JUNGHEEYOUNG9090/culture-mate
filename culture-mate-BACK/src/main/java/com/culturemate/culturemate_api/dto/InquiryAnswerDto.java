package com.culturemate.culturemate_api.dto;

import com.culturemate.culturemate_api.domain.inquiry.InquiryAnswer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class InquiryAnswerDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "InquiryAnswerRequest", description = "1:1 문의 답변 생성/수정 요청 DTO")
    public static class CreateRequest {
        @NotBlank(message = "답변 내용을 입력해주세요.")
        private String content;
    }

    @Getter
    @Builder
    @Schema(name = "InquiryAnswerResponse", description = "1:1 문의 답변 정보 응답 DTO")
    public static class Response {
        private Long answerId;
        private String content;
        private MemberDto.ProfileResponse author;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(InquiryAnswer answer) {
            return Response.builder()
                    .answerId(answer.getId())
                    .content(answer.getContent())
                    .author(MemberDto.ProfileResponse.from(answer.getAuthor()))
                    .createdAt(answer.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
                    .updatedAt(answer.getUpdatedAt() != null ?
                        answer.getUpdatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime() : null)
                    .build();
        }
    }
}