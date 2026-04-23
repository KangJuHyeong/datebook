# Step 1: backend-domain-foundation

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ADR.md`
- `docs/UI_GUIDE.md`
- `phases/0-mvp/index.json`
- `backend/`

이전 step에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업

Backend 도메인 기초, repository, 공통 오류 처리, 시간 유틸리티를 구현한다. Controller API 기능은 이 step에서 최소 헬스체크 외에는 만들지 않는다.

구현 범위:
- `app.domain`에 `User`, `Couple`, `CoupleMember`, `InviteCode`, `Question`, `DailyQuestion`, `Answer`, `ExportRequest`, `ExportItem` JPA entity를 만든다.
- ARCHITECTURE.md의 데이터 모델 제약을 entity annotation과 service-level 검증이 가능한 구조로 반영한다.
- enum은 필요한 경우 `AnswerState`, `PartnerAnswerStatus`, `ExportStatus`처럼 명확한 이름으로 둔다.
- `app.repository`에 각 entity용 Spring Data JPA repository를 만든다.
- `app.common.error`에 `ErrorCode`, `ErrorResponse`, `FieldErrorResponse`, `BusinessException`, `GlobalExceptionHandler`를 구현한다.
- `app.common.time` 또는 기존 패키지에 Asia/Seoul `LocalDate` 계산과 UTC timestamp 정책을 도울 clock provider를 둔다.
- 질문 seed는 로컬 개발에서 바로 쓸 수 있도록 최소 7개 이상 준비한다. seed 방식은 Spring Boot 표준 방식 중 하나를 사용하되 테스트와 충돌하지 않게 한다.

테스트:
- entity/repository 테스트로 unique 제약, 날짜별 질문 조회, 질문 active 정렬 조회를 검증한다.
- 오류 응답 DTO와 ErrorCode 매핑의 기본 테스트를 작성한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. entity가 ARCHITECTURE.md의 컬럼과 관계를 충분히 반영하는지 확인한다.
3. 전역 예외 처리기가 Java exception class, stack trace, SQL error를 응답에 노출하지 않는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 1을 `"completed"`로 바꾸고 `summary`에 구현한 도메인 기반을 한 줄로 요약한다.

## 금지 사항

- 인증, 초대, 답변, export 서비스 로직을 구현하지 마라. 이유: 이후 backend step의 책임이다.
- QueryDSL을 도입하지 마라. 이유: ADR-011에서 초기 필수 의존성에서 제외했다.
- 답변 본문이나 export payload를 로그에 남기지 마라. 이유: 개인정보 보호 규칙 위반이다.
