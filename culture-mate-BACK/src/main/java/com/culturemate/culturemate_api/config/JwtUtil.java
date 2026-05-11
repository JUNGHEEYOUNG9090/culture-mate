package com.culturemate.culturemate_api.config;

import com.culturemate.culturemate_api.domain.member.Member;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}") // 24시간 (milliseconds)
  private Long expiration;

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * JWT 토큰 생성 - AuthenticatedUser 기반
   */
  public String generateToken(AuthenticatedUser user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("loginId", user.getLoginId());
    claims.put("role", user.getRole().name());
    claims.put("status", user.getStatus().name());

    return Jwts.builder()
      .claims(claims)
      .subject(user.getMemberId().toString())
      .issuedAt(new Date())
      .expiration(new Date(System.currentTimeMillis() + expiration))
      .signWith(getSigningKey())
      .compact();
  }

  /**
   * JWT 토큰 생성 - Member 엔티티 기반
   */
  public String generateToken(Member member) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("loginId", member.getLoginId());
    claims.put("role", member.getRole().name());
    claims.put("status", member.getStatus().name());

    return Jwts.builder()
      .claims(claims)
      .subject(member.getId().toString())
      .issuedAt(new Date())
      .expiration(new Date(System.currentTimeMillis() + expiration))
      .signWith(getSigningKey())
      .compact();
  }

  /**
   * 토큰에서 memberId 추출 (subject)
   */
  public Long getMemberIdFromToken(String token) {
    return Long.valueOf(getClaimsFromToken(token).getSubject());
  }

  /**
   * 토큰에서 loginId 추출
   */
  public String getLoginIdFromToken(String token) {
    return getClaimsFromToken(token).get("loginId").toString();
  }

  /**
   * 토큰에서 role 추출
   */
  public String getRoleFromToken(String token) {
    return getClaimsFromToken(token).get("role").toString();
  }

  /**
   * 토큰에서 status 추출
   */
  public String getStatusFromToken(String token) {
    return getClaimsFromToken(token).get("status").toString();
  }

  /**
   * 토큰 유효성 검증 - memberId 기반
   */
  public boolean validateToken(String token, Long memberId) {
    try {
      return getMemberIdFromToken(token).equals(memberId) && !isTokenExpired(token);
    } catch (Exception e) {
      log.error("토큰 검증 실패: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 토큰 유효성 검증 - loginId 기반 (하위 호환성)
   */
  public boolean validateTokenByLoginId(String token, String loginId) {
    try {
      return getLoginIdFromToken(token).equals(loginId) && !isTokenExpired(token);
    } catch (Exception e) {
      log.error("토큰 검증 실패: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 토큰 유효성 검증 - 토큰만으로 검증 (권장)
   */
  public boolean validateToken(String token) {
    try {
      getClaimsFromToken(token);
      return !isTokenExpired(token);
    } catch (Exception e) {
      log.error("토큰 검증 실패: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 토큰에서 Claims 추출
   */
  private Claims getClaimsFromToken(String token) {
    return Jwts.parser()
      .verifyWith(getSigningKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  /**
   * 토큰 만료 여부 확인
   */
  private boolean isTokenExpired(String token) {
    return getClaimsFromToken(token).getExpiration().before(new Date());
  }
}