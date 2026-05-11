package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.dto.InquiryAnswerDto;
import com.culturemate.culturemate_api.dto.InquiryDto;
import com.culturemate.culturemate_api.service.InquiryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Inquiry API (Admin)", description = "1:1 문의 API (관리자용)")
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class InquiryAdminController {

    private final InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<List<InquiryDto.ListResponse>> getAllInquiries(
            @AuthenticationPrincipal AuthenticatedUser admin) {

        var inquiries = inquiryService.getAllInquiries(admin.getMemberId());
        List<InquiryDto.ListResponse> responses = inquiries.stream()
          .map(inquiry -> {
            List<String> imageUrls = inquiryService.getInquiryImagePaths(inquiry.getId());
            return InquiryDto.ListResponse.from(inquiry, imageUrls);
          })
          .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<InquiryDto.Response> createOrUpdateAnswer(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerDto.CreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {

        var inquiry = inquiryService.createOrUpdateAnswer(inquiryId, request, admin.getMemberId());
        List<String> imageUrls = inquiryService.getInquiryImagePaths(inquiry.getId());
        return ResponseEntity.ok(InquiryDto.Response.from(inquiry, imageUrls));
    }
}
