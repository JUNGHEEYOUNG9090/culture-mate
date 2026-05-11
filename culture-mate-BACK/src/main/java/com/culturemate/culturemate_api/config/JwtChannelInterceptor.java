package com.culturemate.culturemate_api.config;

import com.culturemate.culturemate_api.domain.member.MemberStatus;
import com.culturemate.culturemate_api.dto.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

  private final JwtUtil jwtUtil;
  private final UserDetailsService userDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      // WebSocket 연결 시 JWT 토큰 검증
      String token = getTokenFromHeaders(accessor);

      // 핸드셰이크에서 저장된 토큰도 확인
      if (token == null) {
        Object handshakeToken = accessor.getSessionAttributes().get("jwt_token");
        if (handshakeToken instanceof String) {
          token = (String) handshakeToken;
          log.debug("핸드셰이크에서 저장된 토큰 사용");
        }
      }

      if (token == null) {
        // 토큰이 없어도 연결은 허용하되 경고 로그 남김
        String clientIp = (String) accessor.getSessionAttributes().getOrDefault("client_ip", "unknown");
        log.warn("WebSocket 연결 시 JWT 토큰이 없습니다. IP: {}", clientIp);
        return message;
      }

      try {
        if (!jwtUtil.validateToken(token)) {
          log.warn("WebSocket JWT 토큰 검증 실패");
          throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다");
        }

        String loginId = jwtUtil.getLoginIdFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);

        // MemberStatus 검증
        if (userDetails instanceof AuthenticatedUser) {
          AuthenticatedUser authenticatedUser = (AuthenticatedUser) userDetails;

          if (!isAllowedStatus(authenticatedUser.getStatus())) {
            log.warn("비활성 상태 사용자의 WebSocket 연결 시도: {} (상태: {})",
                    loginId, authenticatedUser.getStatus());
            throw new IllegalArgumentException("계정이 비활성 상태입니다");
          }
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());

        accessor.setUser(authentication);
        log.debug("WebSocket JWT 인증 성공: {}", loginId);

      } catch (IllegalArgumentException e) {
        // 인증 실패 시 예외를 다시 throw하여 연결 거부
        throw e;
      } catch (Exception e) {
        log.error("WebSocket JWT 처리 중 오류 발생", e);
        throw new IllegalArgumentException("JWT 토큰 처리 중 오류가 발생했습니다");
      }
    }

    // 메시지 전송/구독 시 인증 확인
    if (StompCommand.SEND.equals(accessor.getCommand()) ||
        StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

      if (accessor.getUser() == null) {
        log.warn("인증되지 않은 사용자의 {} 시도", accessor.getCommand());
        throw new IllegalArgumentException("인증된 사용자만 메시지를 전송하거나 구독할 수 있습니다");
      }

      // 채팅방 권한 검증
      try {
        validateChatRoomAccess(accessor);
      } catch (Exception e) {
        log.warn("채팅방 접근 권한 검증 실패: {}", e.getMessage());
        throw new IllegalArgumentException("채팅방 접근 권한이 없습니다");
      }
    }

    return message;
  }

  /**
   * STOMP 헤더 및 URL 파라미터에서 JWT 토큰 추출
   */
  private String getTokenFromHeaders(StompHeaderAccessor accessor) {
    // 1. Authorization 헤더에서 토큰 추출
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      log.debug("Authorization 헤더에서 토큰 발견");
      return authHeader.substring(7);
    }

    // 2. X-Authorization 헤더에서 토큰 추출
    String xAuthHeader = accessor.getFirstNativeHeader("X-Authorization");
    if (xAuthHeader != null && xAuthHeader.startsWith("Bearer ")) {
      log.debug("X-Authorization 헤더에서 토큰 발견");
      return xAuthHeader.substring(7);
    }

    // 3. token 헤더에서 시도 (클라이언트 호환성)
    String token = accessor.getFirstNativeHeader("token");
    if (token != null && !token.isEmpty()) {
      log.debug("token 헤더에서 토큰 발견");
      return token;
    }

    // 4. access-token 헤더에서 시도
    String accessToken = accessor.getFirstNativeHeader("access-token");
    if (accessToken != null && !accessToken.isEmpty()) {
      log.debug("access-token 헤더에서 토큰 발견");
      return accessToken;
    }

    // 5. SockJS URL 파라미터에서 토큰 추출 시도
    try {
      String urlToken = extractTokenFromUrl(accessor);
      if (urlToken != null && !urlToken.isEmpty()) {
        log.debug("URL 파라미터에서 토큰 발견");
        return urlToken;
      }
    } catch (Exception e) {
      log.debug("URL 파라미터 토큰 추출 실패: {}", e.getMessage());
    }

    log.debug("모든 방법으로 토큰을 찾을 수 없음");
    return null;
  }

  /**
   * SockJS URL에서 token 파라미터 추출
   */
  private String extractTokenFromUrl(StompHeaderAccessor accessor) {
    try {
      // SockJS 연결 정보에서 원본 요청 URL 추출 시도
      Object nativeMessage = accessor.getHeader("nativeMessage");
      if (nativeMessage != null) {
        String messageStr = nativeMessage.toString();
        // URL에서 token 파라미터 추출
        if (messageStr.contains("token=")) {
          int tokenStart = messageStr.indexOf("token=") + 6;
          int tokenEnd = messageStr.indexOf("&", tokenStart);
          if (tokenEnd == -1) {
            tokenEnd = messageStr.indexOf(" ", tokenStart);
          }
          if (tokenEnd == -1) {
            tokenEnd = messageStr.length();
          }

          String urlToken = messageStr.substring(tokenStart, tokenEnd);
          return java.net.URLDecoder.decode(urlToken, "UTF-8");
        }
      }

      // HTTP 헤더에서 원본 요청 URL 추출 시도
      String requestUrl = accessor.getFirstNativeHeader("Sec-WebSocket-Extensions");
      if (requestUrl != null && requestUrl.contains("token=")) {
        // 비슷한 방식으로 토큰 추출
      }

    } catch (Exception e) {
      log.debug("URL 토큰 추출 중 오류: {}", e.getMessage());
    }

    return null;
  }

  /**
   * 클라이언트 IP 추출
   */
  private String getClientIp(StompHeaderAccessor accessor) {
    try {
      return accessor.getFirstNativeHeader("X-Forwarded-For") != null
          ? accessor.getFirstNativeHeader("X-Forwarded-For")
          : accessor.getFirstNativeHeader("X-Real-IP") != null
              ? accessor.getFirstNativeHeader("X-Real-IP")
              : "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }

  /**
   * 채팅방 접근 권한 검증
   */
  private void validateChatRoomAccess(StompHeaderAccessor accessor) {
    try {
      // STOMP destination에서 채팅방 ID 추출
      String destination = accessor.getDestination();
      if (destination == null) {
        return; // destination이 없으면 건너뛰기
      }

      // /topic/chatroom/{roomId} 패턴에서 roomId 추출
      if (destination.startsWith("/topic/chatroom/")) {
        String roomIdStr = destination.substring("/topic/chatroom/".length());
        try {
          Long roomId = Long.parseLong(roomIdStr);
          log.debug("채팅방 {} 접근 권한 검증 - 사용자: {}", roomId,
                    accessor.getUser() != null ? accessor.getUser().getName() : "anonymous");
          // TODO: 실제 권한 검증 로직 구현
          // - 동행 호스트인지 확인
          // - 승인된 참여자인지 확인
          // - 채팅방 멤버인지 확인
        } catch (NumberFormatException e) {
          log.warn("잘못된 채팅방 ID 형식: {}", roomIdStr);
          throw new IllegalArgumentException("잘못된 채팅방 ID 형식");
        }
      }

      // /app/chat.sendMessage 등의 발행 경로는 메시지 본문에서 roomId 확인 필요
      // (현재는 기본 인증 검증만 수행)

    } catch (Exception e) {
      log.error("채팅방 권한 검증 중 오류: {}", e.getMessage());
      throw new IllegalArgumentException("채팅방 권한 검증 실패");
    }
  }

  /**
   * MemberStatus 허용 여부 검증
   */
  private boolean isAllowedStatus(MemberStatus status) {
    return switch (status) {
      case ACTIVE -> true;          // 정상 - 전체 기능 허용
      case DORMANT -> true;         // 휴면 - 기본 기능만 허용 (추후 세분화 가능)
      case SUSPENDED -> false;      // 일시정지 - 접근 차단
      case BANNED -> false;         // 영구정지 - 접근 차단
      case DELETED -> false;        // 탈퇴 - 접근 차단
    };
  }
}