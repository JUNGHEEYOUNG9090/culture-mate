# 멀티 스테이지 빌드로 이미지 크기 최적화
# Stage 1: 빌드 단계
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Gradle 파일을 먼저 복사하여 캐싱 최적화
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (이 레이어는 캐시됨)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (테스트 제외로 빌드 속도 향상)
RUN gradle build -x test --no-daemon

# Stage 2: 실행 단계
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 보안을 위한 non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# 로그 및 이미지 업로드 디렉토리 생성 및 권한 설정
RUN mkdir -p /app/logs /app/images && \
    chown -R spring:spring /app

# 사용자 전환
USER spring:spring

# 빌드 단계에서 생성된 실행 가능한 jar 파일만 복사
COPY --from=builder --chown=spring:spring /app/build/libs/*-SNAPSHOT.jar app.jar

# 기존 이미지 파일이 있다면 복사 (선택적)
# 이미지는 볼륨 마운트로 관리하므로 여기서는 스킵

# 업로드 이미지와 로그를 위한 볼륨 선언
VOLUME ["/app/images", "/app/logs"]

# 포트 노출 (컨테이너 내부 포트, 외부 포트는 docker run -p 로 매핑)
EXPOSE 8080

# 헬스체크 설정 (루트 경로로 간단하게 체크)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# 컨테이너 환경을 위한 JVM 옵션
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 애플리케이션 실행 (환경변수로 프로파일 지정, 기본값: prod)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILE:-prod}"]