package com.culturemate.culturemate_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  @Value("${cors.allowed-methods}")
  private String allowedMethods;

  @Value("${cors.allowed-headers}")
  private String allowedHeaders;

  @Value("${cors.allow-credentials}")
  private boolean allowCredentials;

  @Value("${cors.max-age}")
  private long maxAge;

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize ->
            authorize
                // CORS preflight 요청 허용
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 인증 없이 접근 가능한 엔드포인트
                .requestMatchers(
                    // 인증 API
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/check-login-id",
                    "/api/v1/auth/check-email",
                    // WebSocket 연결 허용
                    "/websocket/**", "/ws/**",
                    // 문서/개발 도구
                    "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs", "/swagger-ui.html", "/favicon.ico",
                    "/api-docs/**",
                    // 이미지 파일 접근 허용
                    "/images/**"
                ).permitAll()
                // 보안이 필요한 개인정보 조회는 인증 필요
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/*/my/**",                    // 내 정보 관련 (my로 시작하는 모든 경로)
                    "/api/v1/members/*/detail",           // 멤버 상세 정보
                    "/api/v1/chatroom/**",                // 채팅방 관련
                    "/api/v1/together/received-applications", // 받은 신청 조회
                    "/api/v1/together/my-applications",   // 내 신청 조회
                    "/api/v1/together/my-interests"       // 관심 동행 조회
                ).authenticated()
                // 모든 GET 요청은 공개 (읽기 전용)
                .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                // 그 외 모든 API는 인증 필요 (POST, PUT, DELETE 등)
                .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // YML에서 가져온 설정값 사용
    String[] origins = allowedOrigins.split(",");
    for (String origin : origins) {
      configuration.addAllowedOrigin(origin.trim());
    }

    String[] methods = allowedMethods.split(",");
    for (String method : methods) {
      configuration.addAllowedMethod(method.trim());
    }

    String[] headers = allowedHeaders.split(",");
    for (String header : headers) {
      configuration.addAllowedHeader(header.trim());
    }

    configuration.setAllowCredentials(allowCredentials);
    configuration.setMaxAge(maxAge);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
