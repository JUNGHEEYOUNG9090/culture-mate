package com.culturemate.culturemate_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;
  
  private final JwtChannelInterceptor jwtChannelInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // JWT 핸드셰이크 인터셉터 생성
    JwtWebSocketHandshakeInterceptor jwtHandshakeInterceptor = new JwtWebSocketHandshakeInterceptor();

    registry.addEndpoint("/websocket")
      .setAllowedOrigins(allowedOrigins.split(","))
      .setAllowedOriginPatterns("*")  // 개발 환경에서 모든 오리진 허용
      .addInterceptors(jwtHandshakeInterceptor)  // JWT 핸드셰이크 인터셉터 추가
      .withSockJS();

    // 네이티브 WebSocket 엔드포인트도 추가 (SockJS 대안)
    registry.addEndpoint("/ws")
      .setAllowedOrigins(allowedOrigins.split(","))
      .setAllowedOriginPatterns("*")
      .addInterceptors(jwtHandshakeInterceptor);  // JWT 핸드셰이크 인터셉터 추가
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // JWT 인터셉터 활성화
    registration.interceptors(jwtChannelInterceptor);
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
    // 메시지 크기 제한
    registry.setMessageSizeLimit(64 * 1024);        // 64KB
    registry.setSendBufferSizeLimit(512 * 1024);    // 512KB
    registry.setSendTimeLimit(20 * 1000);           // 20초
    
    // 연결 타임아웃 설정
    registry.setTimeToFirstMessage(30 * 1000);      // 첫 메시지까지 30초
  }
}
