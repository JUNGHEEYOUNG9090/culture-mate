package com.culturemate.culturemate_api.dto;

import com.culturemate.culturemate_api.domain.inquiry.Inquiry;
import com.culturemate.culturemate_api.domain.inquiry.InquiryCategory;
import com.culturemate.culturemate_api.domain.inquiry.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class InquiryDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "InquiryRequest", description = "1:1 문의 생성 요청 DTO")
    public static class CreateRequest {
        @NotBlank(message = "제목을 입력해주세요.")
        private String title;

        @NotBlank(message = "내용을 입력해주세요.")
        private String content;

        @NotNull(message = "카테고리를 선택해주세요.")
        private InquiryCategory category;
    }

    @Getter
    @Builder
    @Schema(name = "InquiryResponse", description = "1:1 문의 상세 정보 응답 DTO")
    public static class Response {
        private Long inquiryId;
        private String title;
        private String content;
        private MemberDto.ProfileResponse author;
        private InquiryCategory category;
        private InquiryStatus status;
        private LocalDateTime createdAt;
        private InquiryAnswerDto.Response answer;
        private List<String> imageUrls;

        public static Response from(Inquiry inquiry) {
            return Response.builder()
                    .inquiryId(inquiry.getId())
                    .title(inquiry.getTitle())
                    .content(inquiry.getContent())
                    .author(MemberDto.ProfileResponse.from(inquiry.getAuthor()))
                    .category(inquiry.getCategory())
                    .status(inquiry.getStatus())
                    .createdAt(inquiry.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
                    .answer(inquiry.getAnswer() != null ? InquiryAnswerDto.Response.from(inquiry.getAnswer()) : null)
                    .imageUrls(List.of()) // 기본값, 필요시 withImages 메서드 사용
                    .build();
        }

        public static Response from(Inquiry inquiry, List<String> imageUrls) {
            return Response.builder()
                    .inquiryId(inquiry.getId())
                    .title(inquiry.getTitle())
                    .content(inquiry.getContent())
                    .author(MemberDto.ProfileResponse.from(inquiry.getAuthor()))
                    .category(inquiry.getCategory())
                    .status(inquiry.getStatus())
                    .createdAt(inquiry.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
                    .answer(inquiry.getAnswer() != null ? InquiryAnswerDto.Response.from(inquiry.getAnswer()) : null)
                    .imageUrls(imageUrls)
                    .build();
        }
    }
    
    @Getter
    @Builder
    @Schema(name = "InquiryListResponse", description = "1:1 문의 목록 응답 DTO")
    public static class ListResponse {
        private Long inquiryId;
        private String title;
        private String content;
        private MemberDto.ProfileResponse author;
        private InquiryCategory category;
        private InquiryStatus status;
        private LocalDateTime createdAt;
        private InquiryAnswerDto.Response answer;
        private List<String> imageUrls;

        public static ListResponse from(Inquiry inquiry) {
            return ListResponse.builder()
                    .inquiryId(inquiry.getId())
                    .title(inquiry.getTitle())
                    .content(inquiry.getContent())
                    .author(MemberDto.ProfileResponse.from(inquiry.getAuthor()))
                    .category(inquiry.getCategory())
                    .status(inquiry.getStatus())
                    .createdAt(inquiry.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
                    .answer(inquiry.getAnswer() != null
                    ? InquiryAnswerDto.Response.from(inquiry.getAnswer())
                    : null)
                    .imageUrls(List.of()) // 기본값, 필요시 withImages 메서드 사용
                    .build();
        }

        public static ListResponse from(Inquiry inquiry, List<String> imageUrls) {
            return ListResponse.builder()
                    .inquiryId(inquiry.getId())
                    .title(inquiry.getTitle())
                    .content(inquiry.getContent())
                    .author(MemberDto.ProfileResponse.from(inquiry.getAuthor()))
                    .category(inquiry.getCategory())
                    .status(inquiry.getStatus())
                    .createdAt(inquiry.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
                    .answer(inquiry.getAnswer() != null
                    ? InquiryAnswerDto.Response.from(inquiry.getAnswer())
                    : null)
                    .imageUrls(imageUrls)
                    .build();
        }
    }
}