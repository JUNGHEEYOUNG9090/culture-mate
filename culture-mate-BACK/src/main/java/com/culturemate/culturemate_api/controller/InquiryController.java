package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.domain.inquiry.Inquiry;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.dto.InquiryAnswerDto;
import com.culturemate.culturemate_api.dto.InquiryDto;
import com.culturemate.culturemate_api.service.InquiryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Inquiry API (User)", description = "1:1 문의 API (사용자용)")
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InquiryDto.Response> createInquiry(
            @Valid @RequestPart("inquiry") InquiryDto.CreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal AuthenticatedUser user) {

        Inquiry inquiry = inquiryService.createInquiry(request, images, user.getMemberId());
        List<String> imageUrls = inquiryService.getInquiryImagePaths(inquiry.getId());
        return ResponseEntity.ok(InquiryDto.Response.from(inquiry, imageUrls));
    }

  @GetMapping("/my")
  public ResponseEntity<List<InquiryDto.ListResponse>> getMyInquiries(
    @AuthenticationPrincipal AuthenticatedUser user) {

    List<Inquiry> inquiries;
    if(user.getRole() == Role.ADMIN) {
      inquiries = inquiryService.getAllInquiries(user.getMemberId());
    } else {
      inquiries = inquiryService.getMyInquiries(user.getMemberId());
    }

    List<InquiryDto.ListResponse> responses = inquiries.stream()
      .map(inquiry -> {
        List<String> imageUrls = inquiryService.getInquiryImagePaths(inquiry.getId());
        return InquiryDto.ListResponse.from(inquiry, imageUrls);
      })
      .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

//    @GetMapping("/{inquiryId}")
//    public ResponseEntity<InquiryDto.Response> getInquiry(
//            @PathVariable Long inquiryId,
//            @AuthenticationPrincipal AuthenticatedUser user) {
//
//        var inquiry = inquiryService.getInquiry(inquiryId, user.getMemberId());
//        return ResponseEntity.ok(InquiryDto.Response.from(inquiry));
//    }

}
