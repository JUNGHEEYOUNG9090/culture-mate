package com.culturemate.culturemate_api.service;

import com.culturemate.culturemate_api.domain.Image;
import com.culturemate.culturemate_api.domain.ImageTarget;
import com.culturemate.culturemate_api.domain.inquiry.Inquiry;
import com.culturemate.culturemate_api.domain.inquiry.InquiryAnswer;
import com.culturemate.culturemate_api.domain.inquiry.InquiryStatus;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.dto.InquiryAnswerDto;
import com.culturemate.culturemate_api.dto.InquiryDto;
import com.culturemate.culturemate_api.repository.InquiryAnswerRepository;
import com.culturemate.culturemate_api.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final MemberService memberService;
    private final ImageService imageService;

    /**
     * 문의 생성
     */
    @Transactional
    public Inquiry createInquiry(InquiryDto.CreateRequest dto, List<MultipartFile> images, Long memberId) {
        Member author = memberService.findById(memberId);
        Inquiry inquiry = Inquiry.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .author(author)
                .build();

        inquiryRepository.save(inquiry);

      // 이미지가 있으면 업로드 (DB 저장은 ImageService에서 처리)
      if (images != null && !images.isEmpty()) {
        imageService.uploadMultipleImages(
          images,
          ImageTarget.INQUIRY,
          inquiry.getId()
        );
      }

      return inquiry;
    }

    /**
     * 내 문의 목록 조회
     */
    public List<Inquiry> getMyInquiries(Long memberId) {
        Member author = memberService.findById(memberId);
        return inquiryRepository.findByAuthor(author);
    }

    /**
     * 전체 문의 목록 조회 (관리자용)
     */
    public List<Inquiry> getAllInquiries(Long adminId) {
        validateAdmin(adminId);
        return inquiryRepository.findAllWithAuthor();
    }

    /**
     * 문의 상세 조회
     */
    public Inquiry getInquiry(Long inquiryId, Long memberId) {
        Inquiry inquiry = findInquiryById(inquiryId);
        Member member = memberService.findById(memberId);

        // 관리자이거나 본인일 경우에만 조회 가능
        if (member.getRole() != Role.ADMIN && !inquiry.getAuthor().getId().equals(memberId)) {
            throw new AccessDeniedException("해당 문의를 조회할 권한이 없습니다.");
        }
        return inquiry;
    }

    /**
     * 답변 생성 또는 수정 (관리자용)
     */
    @Transactional
    public Inquiry createOrUpdateAnswer(Long inquiryId, InquiryAnswerDto.CreateRequest dto, Long adminId) {
      validateAdmin(adminId);
      Inquiry inquiry = findInquiryById(inquiryId);
      Member admin = memberService.findById(adminId);

      InquiryAnswer answer = inquiry.getAnswer();
      if (answer != null) {
        // 기존 답변 수정
        answer.setContent(dto.getContent());
      } else {
        // 새 답변 생성
        answer = InquiryAnswer.builder()
          .content(dto.getContent())
          .author(admin)
          .build();
        inquiry.setAnswer(answer);  // 상태 변경은 setAnswer()에서 자동 처리
      }

      return inquiryRepository.save(inquiry);
    }

    private Inquiry findInquiryById(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의를 찾을 수 없습니다. ID: " + inquiryId));
    }

    /**
     * 문의 이미지 조회 (필요시 사용)
     */
    public List<Image> getInquiryImages(Long inquiryId) {
        return imageService.getImagesByTargetTypeAndId(ImageTarget.INQUIRY, inquiryId);
    }

    /**
     * 문의 이미지 경로 목록 조회 (DTO 변환용 헬퍼 메서드)
     */
    public List<String> getInquiryImagePaths(Long inquiryId) {
        return getInquiryImages(inquiryId).stream()
                .map(Image::getPath)
                .toList();
    }

    private void validateAdmin(Long memberId) {
        Member member = memberService.findById(memberId);
        if (member.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자 권한이 필요합니다.");
        }
    }
}
