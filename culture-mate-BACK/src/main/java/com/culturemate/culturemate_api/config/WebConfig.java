package com.culturemate.culturemate_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // /images/** 경로를 JAR 내부 리소스 + 외부 파일 시스템으로 이중 매핑
    // 1. 외부 업로드 디렉토리 (JAR 실행 위치 기준)
    // 2. JAR 내부 리소스 (빌드시 포함된 이미지)
    registry.addResourceHandler("/images/**")
        .addResourceLocations(
            "file:./images/",              // 외부: 새로 업로드되는 이미지
            "classpath:/static/images/"    // JAR 내부: 빌드시 포함된 이미지
        )
        .setCachePeriod(3600); // 1시간 캐시
  }
}