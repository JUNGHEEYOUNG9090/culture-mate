# Culture-Mate Backend API

> 문화 이벤트 및 커뮤니티 플랫폼의 백엔드 API 서버

## 🚀 프로젝트 개요

Culture-Mate Backend는 문화 이벤트 발견, 그룹 활동 관리, 커뮤니티 토론, 이벤트 리뷰 기능을 제공하는 Spring Boot 기반의 REST API 서버입니다.

## 📋 주요 기능

- **회원 관리**: JWT 기반 인증 및 권한 관리
- **이벤트 관리**: 문화 이벤트 등록, 조회, 리뷰 시스템
- **투게더(Together)**: 그룹 활동 생성 및 참가자 관리
- **커뮤니티**: 게시판 및 댓글 시스템
- **실시간 채팅**: WebSocket 기반 채팅 기능
- **관리자 기능**: 사용자 및 콘텐츠 관리

## 🛠 기술 스택

### 프레임워크 & 언어
- **Java 21**: 최신 Java LTS 버전
- **Spring Boot 3.5.5**: 메인 프레임워크
- **Spring Data JPA**: 데이터 접근 계층
- **Spring Security**: 보안 및 인증
- **Spring WebSocket**: 실시간 통신

### 데이터베이스
- **H2**: 개발 환경
- **Oracle Database**: 운영 환경

### 보안 & 인증
- **JWT (JSON Web Token)**: 토큰 기반 인증
- **JJWT 0.12.5**: JWT 라이브러리

### 문서화 & 개발
- **SpringDoc OpenAPI 2.8.12**: API 문서 자동 생성
- **Lombok**: 코드 생성 자동화
- **Spring Boot DevTools**: 개발 도구

## 🏗 프로젝트 구조

```
src/main/java/com/culturemate/culturemate_api/
├── config/                    # 설정 클래스들
│   ├── CorsConfig.java       # CORS 설정
│   ├── SecurityConfig.java   # Spring Security 설정
│   ├── JwtUtil.java         # JWT 유틸리티
│   ├── WebSocketConfig.java  # WebSocket 설정
│   └── OpenApiConfig.java    # Swagger 설정
├── controller/               # REST API 컨트롤러
│   ├── AuthApiController.java
│   ├── EventController.java
│   ├── TogetherController.java
│   ├── BoardController.java
│   └── ChatController.java
├── service/                  # 비즈니스 로직
├── repository/               # 데이터 접근 계층
├── domain/                   # 도메인 엔티티
│   ├── member/              # 회원 관리
│   ├── event/               # 이벤트 관리
│   ├── together/            # 투게더 기능
│   ├── community/           # 커뮤니티
│   └── statistics/          # 통계 및 태그
├── dto/                     # 데이터 전송 객체
└── exception/               # 예외 처리
```

## 🚀 시작하기

### 사전 요구사항
- Java 21 이상
- Gradle 7.0 이상

### 환경 변수 설정

`.env` 파일에 다음 환경 변수를 설정하세요:

```env
# 개발 환경 (H2 Database)
# 자동으로 H2 인메모리 데이터베이스 사용

# 운영 환경 (Oracle Database)
DB_URL=jdbc:oracle:thin:@//your-host:port/service
DB_USERNAME=your-username
DB_PASSWORD=your-password
DB_DDL_AUTO=update
```

### 서버 실행

```bash
# 1. 프로젝트 클론
git clone [repository-url]
cd culture-mate-BACK

# 2. 의존성 설치 및 빌드
./gradlew build

# 3. 서버 실행
./gradlew bootRun
```

서버가 성공적으로 시작되면 `http://localhost:8080`에서 접속할 수 있습니다.

## 🔧 개발 명령어

```bash
# 서버 실행 (개발 모드)
./gradlew bootRun

# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests ClassName

# 빌드 파일 정리
./gradlew clean

# Java 소스 컴파일 (디버깅용)
./gradlew compileJava
```

## 📚 API 문서

서버 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## 🗄 데이터베이스

### 개발 환경 (H2)
- **콘솔**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **사용자명**: `sa`
- **비밀번호**: (공백)

### 운영 환경 (Oracle)
- Oracle Database 11g 이상 지원
- 환경 변수를 통한 연결 설정

## 🔐 보안 설정

### JWT 토큰
- **인증 헤더**: `Authorization: Bearer <token>`
- **토큰 만료**: 24시간 (설정 가능)
- **리프레시 토큰**: 지원

### CORS 설정
- 프론트엔드 (`http://localhost:3000`) 허용
- 인증이 필요한 요청에 대한 Credentials 지원

## 🌐 API 엔드포인트

### 인증 API
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/register` - 회원가입
- `POST /api/v1/auth/logout` - 로그아웃

### 이벤트 API
- `GET /api/v1/events` - 이벤트 목록 조회
- `GET /api/v1/events/{id}` - 이벤트 상세 조회
- `POST /api/v1/events` - 이벤트 생성

### 투게더 API
- `GET /api/v1/together` - 투게더 목록 조회
- `POST /api/v1/together` - 투게더 생성
- `POST /api/v1/together/{id}/join` - 투게더 참가

### 커뮤니티 API
- `GET /api/v1/board` - 게시글 목록 조회
- `POST /api/v1/board` - 게시글 작성
- `GET /api/v1/comments/{boardId}` - 댓글 조회

## 🔄 실시간 기능

### WebSocket 연결
- **엔드포인트**: `ws://localhost:8080/websocket`
- **인증**: JWT 토큰 기반
- **채팅방**: STOMP 프로토콜 사용

## 📊 로깅

- **파일 위치**: `server.log`
- **실시간 로깅**: 콘솔 및 파일 동시 출력
- **로그 레벨**: 개발 환경에서 DEBUG 레벨

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests MemberServiceTest

# 특정 테스트 메서드 실행
./gradlew test --tests MemberServiceTest.testCreateMember
```

## 🐛 알려진 이슈

- **순환 의존성**: Service 클래스 간 의존성 문제가 발생할 경우 `@Lazy` 어노테이션 사용
- **이미지 처리**: IOException을 RuntimeException으로 래핑하여 처리
- **WebSocket 인증**: JwtChannelInterceptor를 통한 커스텀 인증 구현

## 📄 라이선스

이 프로젝트는 개인 학습 및 포트폴리오 목적으로 제작되었습니다.

## 👨‍💻 개발자

- **개발자**: [개발자명]
- **이메일**: [이메일]
- **GitHub**: [GitHub URL]

---

## 🔗 관련 프로젝트

- **Frontend**: [culture-mate-FRONT](../culture-mate-FRONT/) - Next.js 기반 프론트엔드 애플리케이션