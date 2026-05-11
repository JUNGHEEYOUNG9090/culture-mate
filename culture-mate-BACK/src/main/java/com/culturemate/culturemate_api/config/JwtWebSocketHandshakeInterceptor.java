package com.culturemate.culturemate_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 핸드셰이크 시 JWT 토큰 추출 및 저장
 */
@Slf4j
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) throws Exception {

    log.debug("WebSocket 핸드셰이크 시작");

    try {
      String token = extractTokenFromRequest(request);

      if (token != null && !token.isEmpty()) {
        log.debug("핸드셰이크에서 JWT 토큰 발견 - 길이: {}", token.length());
        // attributes에 저장하여 STOMP 연결 시 사용 가능하도록 함
        attributes.put("jwt_token", token);
        attributes.put("authenticated", true);
      } else {
        log.debug("핸드셰이크에서 JWT 토큰을 찾을 수 없음");
        attributes.put("authenticated", false);
      }

      // 클라이언트 IP 저장
      String clientIp = getClientIpAddress(request);
      attributes.put("client_ip", clientIp);
      log.debug("클라이언트 IP: {}", clientIp);

    } catch (Exception e) {
      log.error("핸드셰이크 처리 중 오류: {}", e.getMessage(), e);
    }

    // 핸드셰이크 계속 진행 (토큰이 없어도 연결 허용)
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {

    if (exception != null) {
      log.error("WebSocket 핸드셰이크 완료 후 오류: {}", exception.getMessage());
    } else {
      log.debug("WebSocket 핸드셰이크 성공적으로 완료");
    }
  }

  /**
   * 여러 방법으로 JWT 토큰 추출 시도
   */
  private String extractTokenFromRequest(ServerHttpRequest request) {
    try {
      // 1. URL 파라미터에서 토큰 추출
      URI uri = request.getURI();
      String query = uri.getQuery();

      if (query != null) {
        // access_token 파라미터
        if (query.contains("access_token=")) {
          String token = extractQueryParam(query, "access_token");
          if (token != null) {
            log.debug("URL access_token 파라미터에서 토큰 발견");
            return java.net.URLDecoder.decode(token, "UTF-8");
          }
        }

        // token 파라미터
        if (query.contains("token=")) {
          String token = extractQueryParam(query, "token");
          if (token != null) {
            log.debug("URL token 파라미터에서 토큰 발견");
            return java.net.URLDecoder.decode(token, "UTF-8");
          }
        }
      }
    } catch (Exception e) {
      log.debug("URL 파라미터 토큰 추출 중 오류: {}", e.getMessage());
    }

    // 2. Authorization 헤더에서 토큰 추출
    String authHeader = request.getHeaders().getFirst("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      log.debug("Authorization 헤더에서 토큰 발견");
      return authHeader.substring(7);
    }

    // 3. 기타 헤더들에서 토큰 추출
    String[] tokenHeaders = {"X-Auth-Token", "token", "access_token", "jwt"};
    for (String headerName : tokenHeaders) {
      String headerValue = request.getHeaders().getFirst(headerName);
      if (headerValue != null && !headerValue.isEmpty()) {
        log.debug("{} 헤더에서 토큰 발견", headerName);
        return headerValue;
      }
    }

    // 4. WebSocket Sub-Protocol에서 토큰 추출
    String subProtocolHeader = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
    if (subProtocolHeader != null) {
      if (subProtocolHeader.startsWith("access_token.")) {
        String token = subProtocolHeader.substring("access_token.".length());
        log.debug("WebSocket Sub-Protocol에서 토큰 발견");
        return token;
      }
      if (subProtocolHeader.startsWith("jwt.")) {
        String token = subProtocolHeader.substring("jwt.".length());
        log.debug("JWT Sub-Protocol에서 토큰 발견");
        return token;
      }
    }

    return null;
  }

  /**
   * URL 쿼리 파라미터에서 특정 값 추출
   */
  private String extractQueryParam(String query, String paramName) {
    String searchStr = paramName + "=";
    int startIndex = query.indexOf(searchStr);

    if (startIndex == -1) {
      return null;
    }

    startIndex += searchStr.length();
    int endIndex = query.indexOf("&", startIndex);

    if (endIndex == -1) {
      endIndex = query.length();
    }

    return query.substring(startIndex, endIndex);
  }

  /**
   * 클라이언트 IP 주소 추출
   */
  private String getClientIpAddress(ServerHttpRequest request) {
    if (request instanceof ServletServerHttpRequest) {
      var servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

      // Proxy를 통한 접근인 경우
      String xForwardedFor = servletRequest.getHeader("X-Forwarded-For");
      if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim();
      }

      String xRealIP = servletRequest.getHeader("X-Real-IP");
      if (xRealIP != null && !xRealIP.isEmpty()) {
        return xRealIP;
      }

      return servletRequest.getRemoteAddr();
    }

    return "unknown";
  }
}