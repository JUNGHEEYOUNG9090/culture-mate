# 🎭 Culture-Mate: 문화 생활 통합 플랫폼
> **Full-stack Project (Spring Boot 3.5 & Next.js 15 Integration)**

'Culture-Mate'는 사용자가 문화 이벤트를 발견하고, 소모임(Together)을 통해 함께 활동하며, 실시간 채팅으로 소통할 수 있는 모던 웹 애플리케이션입니다. 본 저장소는 백엔드와 프론트엔드 코드를 통합하여 관리합니다.

---

## 🛠 전체 기술 스택 (System Tech Stack)

### [Backend]
- **Framework:** Java 21, Spring Boot 3.5.5
- **Database:** Oracle Database (운영), H2 (개발)
- **Security:** Spring Security & JWT (JJWT 0.12.5)
- **Real-time:** Spring WebSocket (STOMP)
- **API Docs:** SpringDoc OpenAPI 2.8.12 (Swagger)

### [Frontend]
- **Framework:** Next.js 15.4.4 (App Router), React 19.1.0
- **Build Tool:** Turbopack
- **Styling:** Tailwind CSS 4.1.11 (Responsive Design)
- **Communication:** Axios 1.11.0, @stomp/stompjs 7.1.1

---

## 📂 프로젝트 구조 (Project Structure)

- [**backend/**](./backend): Spring Boot 기반 REST API 서버
- [**frontend/**](./frontend): Next.js 기반 클라이언트 웹 애플리케이션
- [**docs/**](./docs): 아키텍처 설계서 및 프로젝트 관련 문서 자료

---

## 📋 핵심 기능 (Key Features)

1. **이벤트 서비스**: 문화 이벤트 검색, 상세 조회 및 리뷰 시스템
2. **투게더(Together)**: 취향 기반 그룹 활동 생성 및 참여 관리
3. **커뮤니티**: 게시판 및 댓글 시스템을 통한 사용자 간 소통
4. **실시간 채팅**: STOMP 프로토콜 기반의 실시간 채팅방 운영
5. **인증/보안**: JWT 기반의 보안 인증 및 회원 관리 시스템

---

## 📖 파트별 상세 가이드

상세한 환경 설정 및 실행 방법은 각 폴더의 문서를 참고해 주세요.

* **[🚀 Backend 상세 가이드 바로가기](./backend/README.md)**: API 명세, DB 구조, 서버 명령어
* **[🎨 Frontend 상세 가이드 바로가기](./frontend/README.md)**: 컴포넌트 구조, 라우팅, 디자인 시스템

---

## 🚀 시작하기 (Quick Start)

### 1. Backend 서버 실행
```bash
cd backend
./gradlew bootRun