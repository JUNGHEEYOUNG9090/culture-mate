package com.culturemate.culturemate_api.config;

import com.culturemate.culturemate_api.domain.member.MemberStatus;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import com.culturemate.culturemate_api.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final AuthService authService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String token = getTokenFromRequest(request);

    // 토큰이 있고 아직 인증되지 않은 경우에만 처리
    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        if (jwtUtil.validateToken(token)) {
          String loginId = jwtUtil.getLoginIdFromToken(token);
          UserDetails userDetails = authService.loadUserByUsername(loginId);

          // MemberStatus 검증 추가
          if (userDetails instanceof AuthenticatedUser) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) userDetails;

            if (!isAllowedStatus(authenticatedUser.getStatus())) {
              log.warn("비활성 상태 사용자의 접근 시도: {} (상태: {})",
                      loginId, authenticatedUser.getStatus());
              response.setStatus(HttpServletResponse.SC_FORBIDDEN);
              response.getWriter().write("{\"error\": \"계정이 비활성 상태입니다.\"}");
              return;
            }
          }

          UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
              userDetails, null, userDetails.getAuthorities());

          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);

          log.debug("JWT 인증 성공: {}", loginId);
        } else {
          log.debug("유효하지 않은 JWT 토큰 - 인증 없이 계속 진행");
        }
      } catch (Exception e) {
        log.debug("JWT 토큰 파싱 실패: {} - 인증 없이 계속 진행", e.getMessage());
        // 토큰 검증 실패 시 로그만 남기고 계속 진행 (인증되지 않은 상태로)
        // SecurityConfig의 permitAll() 설정에 따라 접근 허용 여부가 결정됨
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * HTTP 요청에서 JWT 토큰 추출
   */
  private String getTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  /**
   * MemberStatus 허용 여부 검증
   */
  private boolean isAllowedStatus(MemberStatus status) {
    if (status == null) {
      return true;  // null인 경우 기본적으로 허용 (기존 데이터 호환성)
    }
    return switch (status) {
      case ACTIVE -> true;          // 정상 - 전체 기능 허용
      case DORMANT -> true;         // 휴면 - 기본 기능만 허용 (추후 세분화 가능)
      case SUSPENDED -> false;      // 일시정지 - 접근 차단
      case BANNED -> false;         // 영구정지 - 접근 차단
      case DELETED -> false;        // 탈퇴 - 접근 차단
    };
  }
}