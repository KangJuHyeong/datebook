# Step 4: backend-daily-answers

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ADR.md`
- `phases/0-mvp/index.json`
- `backend/`

이전 step의 인증, 커플 소유권 검증, 도메인 모델을 재사용하라.

## 작업

오늘 질문 배정과 답변 작성/수정 backend API를 구현한다.

API 계약:
- `GET /api/questions/today`
  - 로그인했고 커플에 속한 사용자만 호출할 수 있다.
  - Asia/Seoul 기준 오늘 날짜의 DailyQuestion을 반환한다.
  - 없으면 active 질문 풀에서 sort_order, id 순환 규칙으로 하나를 배정한다.
  - response는 ARCHITECTURE.md의 `dailyQuestionId`, `date`, `answerState`, `question`, `myAnswer`, `partnerAnswer`, `isFullyAnswered` 계약을 따른다.
- `POST /api/answers`
  - request: `dailyQuestionId`, `content`
  - 현재 사용자 자신의 답변을 생성한다. 같은 DailyQuestion에 이미 답변이 있으면 MVP 기본값에 따라 기존 답변 수정으로 처리한다.
- `PUT /api/answers/{answerId}`
  - request: `content`
  - 현재 사용자 자신의 답변만 수정한다.

구현 요구:
- 답변 content는 1자 이상 2000자 이하로 검증한다.
- 상대가 답했지만 내가 답하지 않은 상태에서는 상대 답변 content를 절대 응답에 포함하지 않는다.
- 둘 다 답한 경우에만 partnerAnswer content를 응답한다.
- DailyQuestion 동시 생성은 unique 제약으로 보호하고, 충돌 시 기존 DailyQuestion을 재조회한다.
- 질문 seed가 비어 있으면 `CONFIGURATION_ERROR`로 처리한다.

테스트:
- 같은 커플 같은 날짜에는 같은 질문이 반환되는지 검증한다.
- 질문 순환 배정 규칙을 검증한다.
- NOT_ANSWERED, MY_ANSWERED_PARTNER_WAITING, PARTNER_ANSWERED_ME_WAITING, BOTH_ANSWERED 응답 상태를 검증한다.
- 잠금 상태에서 partnerAnswer content가 없는지 검증한다.
- 본인 답변 수정 성공과 타인 답변 수정 거부를 검증한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. 동시 공개 규칙이 서버 응답 계약에서 보장되는지 확인한다.
3. Asia/Seoul 날짜 기준과 UTC timestamp 정책이 섞이지 않았는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 4를 `"completed"`로 바꾸고 `summary`에 오늘 질문과 답변 API 산출물을 한 줄로 요약한다.

## 금지 사항

- 런타임 AI 질문 생성을 구현하지 마라. 이유: ADR-004와 AGENTS.md에서 금지했다.
- 잠금 상태의 상대 답변 content를 null, 빈 문자열, 마스킹 문자열로도 내려주지 마라. 이유: ADR-012는 content 제거를 요구한다.
- frontend 파일을 수정하지 마라. 이유: 이 step은 backend API 구현만 담당한다.
