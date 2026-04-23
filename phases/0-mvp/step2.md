# Step 2: backend-auth-session

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ADR.md`
- `phases/0-mvp/index.json`
- `backend/`

이전 step에서 만들어진 도메인, repository, 오류 처리 코드를 읽고 일관성을 유지하라.

## 작업

Spring 서버 세션 기반 인증 API를 구현한다. 이 step은 backend 인증 계층만 다룬다.

API 계약:
- `POST /api/auth/signup`
  - request: `email`, `password`, `displayName`
  - response: `id`, `email`, `displayName`
  - 성공 후 서버 세션에 `userId`를 저장한다.
- `POST /api/auth/login`
  - request: `email`, `password`
  - response: `id`, `email`, `displayName`, `coupleId`
  - 실패 시 이메일 존재 여부를 노출하지 않는 `LOGIN_FAILED` 오류를 반환한다.
- `GET /api/auth/me`
  - 로그인 사용자 정보를 반환한다.
  - 미로그인 시 `AUTH_REQUIRED`를 반환한다.
- `POST /api/auth/logout`
  - 서버 세션을 invalidate하고 `{ "success": true }`를 반환한다.

구현 요구:
- 비밀번호는 BCrypt로 해시하고 평문 저장을 금지한다.
- 세션에는 `userId`만 저장한다.
- Spring Security 또는 명시적 config를 사용해 CORS `http://localhost:3000` credentials 요청을 허용한다.
- Controller는 요청/응답 변환만 담당하고 핵심 판단은 service에 둔다.
- Validation은 Bean Validation을 사용하고 전역 예외 처리기의 `VALIDATION_ERROR` 응답으로 매핑한다.

테스트:
- 회원가입 성공, 중복 이메일 실패, 이메일 형식 실패, 비밀번호 길이 실패, 표시 이름 길이 실패를 검증한다.
- 로그인 성공/실패, 현재 사용자 조회, 로그아웃을 controller 또는 integration test로 검증한다.
- 로그인 실패 응답이 이메일 존재 여부를 드러내지 않는지 확인한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. 인증 API가 ARCHITECTURE.md의 API 계약과 오류 전략을 따르는지 확인한다.
3. 세션 쿠키 기반 인증이며 JWT나 소셜 로그인을 추가하지 않았는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 2를 `"completed"`로 바꾸고 `summary`에 인증 API 산출물을 한 줄로 요약한다.

## 금지 사항

- 초대 코드나 커플 연결 로직을 구현하지 마라. 이유: step 3의 책임이다.
- 세션에 이메일, 표시 이름, 답변 본문 같은 추가 정보를 저장하지 마라. 이유: 세션 저장 규칙 위반이다.
- controller에서 try/catch로 개별 오류 응답을 만들지 마라. 이유: 전역 예외 처리 규칙 위반이다.
