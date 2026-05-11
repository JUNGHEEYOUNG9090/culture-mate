package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.domain.member.MemberStatus;
import com.culturemate.culturemate_api.domain.member.Role;
import com.culturemate.culturemate_api.dto.MemberDto;
import com.culturemate.culturemate_api.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;


@Tag(name = "Member API", description = "회원 관리 API")
@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {
  private final MemberService memberService;

  // 회원 삭제
  @DeleteMapping("/{memberId}")
  public ResponseEntity<Void> deleteMember(@PathVariable Long memberId) {
    memberService.delete(memberId);
    return ResponseEntity.noContent().build();
  }

  // 통합 회원 조회 API (쿼리 파라미터 기반)
  @GetMapping
  public ResponseEntity<?> getMember(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String loginId,
      @RequestParam(required = false) MemberStatus status
  ) {
    // ID로 단일 회원 조회
    if (id != null) {
      return ResponseEntity.ok(MemberDto.Response.from(memberService.findById(id)));
    }
    
    // 로그인 ID로 단일 회원 조회
    if (loginId != null) {
      return ResponseEntity.ok(MemberDto.Response.from(memberService.findByLoginId(loginId)));
    }
    
    // 상태별 회원 목록 조회
    if (status != null) {
      return ResponseEntity.ok(
        memberService.findByStatus(status)
          .stream()
          .map(MemberDto.Response::from)
          .collect(Collectors.toList())
      );
    }
    
    // 기본: 전체 회원 조회
    return ResponseEntity.ok(
      memberService.findAll()
        .stream()
        .map(MemberDto.Response::from)
        .collect(Collectors.toList())
    );
  }

  // 회원 상태 변경 (관리자용)
  @PatchMapping("/{id}/status")
  public ResponseEntity<MemberDto.Response> updateMemberStatus(
    @PathVariable Long id,
    @RequestParam MemberStatus status
  ) {
    Member updatedMember = memberService.updateStatus(id, status);
    return ResponseEntity.ok(MemberDto.Response.from(updatedMember));
  }

  // 비밀번호 변경
  @PatchMapping("/{id}/password")
  public ResponseEntity<MemberDto.Response> updateMemberPassword(
    @PathVariable Long id,
    @RequestParam String newPassword
  ) {
    Member updatedMember = memberService.updatePassword(id, newPassword);
    return ResponseEntity.ok(MemberDto.Response.from(updatedMember));
  }

  // 권한 변경 (관리자용)
  @PatchMapping("/{id}/role")
  public ResponseEntity<MemberDto.Response> updateMemberRole(
    @PathVariable Long id,
    @RequestParam Role role
  ) {
    Member updatedMember = memberService.updateRole(id, role);
    return ResponseEntity.ok(MemberDto.Response.from(updatedMember));
  }

}
