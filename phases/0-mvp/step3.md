# Step 3: backend-couple-invites

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ADR.md`
- `phases/0-mvp/index.json`
- `backend/`

이전 step의 인증 세션 코드와 도메인 모델을 읽고 현재 사용자 식별 방식을 재사용하라.

## 작업

커플 생성과 초대 코드 참여 backend API를 구현한다.

API 계약:
- `POST /api/couples`
  - 로그인 사용자가 커플이 없으면 새 Couple과 CoupleMember를 만들고 24시간 유효한 InviteCode를 발급한다.
  - response: `coupleId`, `inviteCode`, `expiresAt`
- `POST /api/couples/join`
  - request: `inviteCode`
  - 유효한 초대 코드로 커플에 참여한다.
  - response: `coupleId`, `memberCount`

구현 요구:
- 보호 API는 현재 세션 사용자 기준으로 동작한다.
- 한 사용자는 하나의 커플에만 속할 수 있다.
- 커플은 최대 2명만 허용한다.
- 초대 코드는 24시간 동안 유효하며, 사용 완료 후 재사용할 수 없다.
- 이미 커플에 속한 사용자는 다른 커플에 참여할 수 없다.
- service 계층에서 커플 소유권과 상태 검증을 수행한다.
- 오류는 `INVITE_CODE_INVALID`, `COUPLE_FULL`, `ALREADY_IN_COUPLE`, `AUTH_REQUIRED` 등 기존 ErrorCode 체계로 반환한다.

테스트:
- 커플 생성 성공, 이미 커플에 속한 사용자 생성 실패를 검증한다.
- 초대 코드 참여 성공, 만료 코드, 사용된 코드, 없는 코드, 커플 정원 초과, 이미 커플 소속 사용자 실패를 검증한다.
- 세션 미인증 요청이 401로 처리되는지 검증한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. 커플 생성과 참여가 최대 2명 규칙을 지키는지 확인한다.
3. 다른 커플 데이터 접근 가능성을 만들지 않았는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 3을 `"completed"`로 바꾸고 `summary`에 커플 초대 API 산출물을 한 줄로 요약한다.

## 금지 사항

- 오늘 질문, 답변, 기록, export 로직을 구현하지 마라. 이유: 이후 step의 책임이다.
- 초대 코드를 로그에 과도하게 남기지 마라. 이유: 초대 코드는 접근 권한에 준하는 민감 정보다.
- 커플 3명 이상 또는 다중 커플 기능을 추가하지 마라. 이유: MVP 범위 밖이다.
