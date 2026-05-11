package com.culturemate.culturemate_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Value("${custom.swagger.server-url:http://localhost:8080}")
  private String serverUrl;

  @Value("${custom.swagger.server-description:개발 서버}")
  private String serverDescription;

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("CultureMate API")
            .version("v1.0.0")
            .description("컬쳐메이트 플랫폼의 백엔드 REST API 문서입니다.\n\n" +
                "이 API는 문화 이벤트 검색, 그룹 모임(Together) 관리, 커뮤니티 기능, 회원 관리를 제공합니다.\n\n" +
                "### 주요 기능\n" +
                "- **회원 관리**: 회원가입, 로그인, 프로필 관리\n" +
                "- **이벤트**: 문화 이벤트 검색, 상세 조회, 관심 등록\n" +
                "- **동행모집**: 그룹 모임 생성, 참여 신청, 승인 관리\n" +
                "- **커뮤니티**: 게시판, 댓글 시스템\n" +
                "- **채팅**: 실시간 채팅 및 채팅방 관리\n\n" +
                "### 인증 방식\n" +
                "JWT Bearer Token을 사용합니다. 로그인 후 받은 토큰을 Authorization 헤더에 포함해 주세요.")
//            .contact(new Contact()
//                .name("CultureMate Development Team")
//                .email("dev@culturemate.com")
//                .url("https://github.com/culture-mate"))
//            .license(new License()
//                .name("MIT License")
//                .url("https://opensource.org/licenses/MIT"))
                )
        .servers(List.of(
            new Server().url(serverUrl).description(serverDescription)
            ))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 토큰을 Authorization 헤더에 Bearer 형식으로 전달\n\n" +
                    "예시: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
            .addSchemas("ErrorResponse", new Schema<>()
                .type("object")
                .description("API 에러 응답 형식")
                .addProperty("status", new StringSchema().example("error").description("응답 상태"))
                .addProperty("message", new StringSchema().example("요청한 리소스를 찾을 수 없습니다").description("에러 메시지"))
                .addProperty("timestamp", new StringSchema().example("2024-01-01T10:00:00").description("에러 발생 시간")))
            .addSchemas("SuccessResponse", new Schema<>()
                .type("object")
                .description("API 성공 응답 형식")
                .addProperty("status", new StringSchema().example("success").description("응답 상태"))
                .addProperty("message", new StringSchema().example("요청이 성공적으로 처리되었습니다").description("성공 메시지"))
                .addProperty("data", new Schema<>().description("응답 데이터"))));
  }
}
