package com.culturemate.culturemate_api.controller;

import com.culturemate.culturemate_api.config.JwtUtil;
import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.dto.MemberDto;
import com.culturemate.culturemate_api.service.AuthService;
import com.culturemate.culturemate_api.service.MemberService;
import com.culturemate.culturemate_api.service.MemberDetailService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Auth API", description = "인증 및 인가 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final AuthService authService;
  private final MemberService memberService;
  private final MemberDetailService memberDetailService;
  private final PasswordEncoder passwordEncoder;

  @Operation(summary = "로그인", description = "회원 로그인을 수행하고 JWT 토큰을 반환합니다")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "로그인 성공 - JWT 토큰과 사용자 정보 반환"),
    @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  @PostMapping("/login")
  public ResponseEntity<?> login(
    @Parameter(description = "로그인 정보", required = true) @RequestBody MemberDto.Login loginRequest) {
    String loginId = loginRequest.getLoginId();
    log.info("로그인 시도: {}", loginId);

    try {
      Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginId, loginRequest.getPassword())
      );

      AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

      // JWT 토큰 생성
      String token = jwtUtil.generateToken(authenticatedUser);

      MemberDto.Response userResponse = MemberDto.Response.builder()
        .id(authenticatedUser.getMemberId())
        .loginId(authenticatedUser.getUsername())
        .role(authenticatedUser.getRole())
        .status(authenticatedUser.getStatus())
        .build();

      // 토큰과 사용자 정보 함께 반환
      Map<String, Object> response = new HashMap<>();
      response.put("token", token);
      response.put("user", userResponse);
      response.put("message", "로그인 성공");

      log.info("로그인 성공: {} (Role: {})", loginId, authenticatedUser.getRole());
      return ResponseEntity.ok(response);

    } catch (AuthenticationException e) {
      log.warn("로그인 실패: {} - {}", loginId, e.getClass().getSimpleName());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("status", "error");
      errorResponse.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
      return ResponseEntity.status(401).body(errorResponse);
    }
  }

  @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "가입 성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
    @ApiResponse(responseCode = "409", description = "이미 존재하는 회원")
  })
  @PostMapping("/register")
  public ResponseEntity<MemberDto.Response> register(
    @Parameter(description = "회원 가입 정보", required = true) @Valid @RequestBody MemberDto.Register registerDto) {
    log.info("회원가입 API 호출: {}", registerDto.getLoginId());

    try {
      Member savedMember = memberService.register(registerDto);
      log.info("회원가입 API 성공: {} (ID: {})", savedMember.getLoginId(), savedMember.getId());
      return ResponseEntity.status(201).body(MemberDto.Response.from(savedMember));
    } catch (Exception e) {
      log.warn("회원가입 API 실패: {} - {}", registerDto.getLoginId(), e.getMessage());
      throw e;
    }
  }

  @Operation(summary = "아이디 중복 확인", description = "로그인 아이디 중복 여부를 확인합니다")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "중복 확인 완료"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
  })
  @GetMapping("/check-login-id")
  public ResponseEntity<Map<String, Object>> checkLoginIdDuplicate(
    @Parameter(description = "확인할 로그인 아이디", required = true) @RequestParam String loginId) {

    boolean isDuplicate = memberService.existsByLoginId(loginId);

    Map<String, Object> response = new HashMap<>();
    response.put("isDuplicate", isDuplicate);
    response.put("message", isDuplicate ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "이메일 중복 확인", description = "이메일 중복 여부를 확인합니다")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "중복 확인 완료"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
  })
  @GetMapping("/check-email")
  public ResponseEntity<Map<String, Object>> checkEmailDuplicate(
    @Parameter(description = "확인할 이메일", required = true) @RequestParam String email) {

    boolean isDuplicate = memberDetailService.existsByEmail(email);

    Map<String, Object> response = new HashMap<>();
    response.put("isDuplicate", isDuplicate);
    response.put("message", isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.");

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "현재 비밀번호 확인", description = "현재 비밀번호의 일치 여부를 확인합니다")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "비밀번호 확인 완료"),
    @ApiResponse(responseCode = "400", description = "비밀번호 불일치"),
    @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  @PostMapping("/verify-current-password")
  public ResponseEntity<Map<String, Object>> verifyCurrentPassword(
    @Parameter(description = "현재 비밀번호", required = true) @Valid @RequestBody MemberDto.VerifyPasswordRequest request,
    @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

    try {
      // 현재 사용자의 Member 엔티티 조회
      Member member = memberService.findById(authenticatedUser.getMemberId());

      // 입력받은 비밀번호와 저장된 암호화된 비밀번호 비교
      boolean isPasswordValid = passwordEncoder.matches(request.getCurrentPassword(), member.getPassword());

      Map<String, Object> response = new HashMap<>();
      response.put("isValid", isPasswordValid);
      response.put("message", isPasswordValid ? "비밀번호가 확인되었습니다." : "현재 비밀번호가 일치하지 않습니다.");

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      Map<String, Object> response = new HashMap<>();
      response.put("isValid", false);
      response.put("message", "비밀번호 확인 중 오류가 발생했습니다.");
      return ResponseEntity.status(500).body(response);
    }
  }
}
