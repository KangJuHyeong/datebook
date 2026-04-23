# Step 5: backend-diary-export

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ADR.md`
- `phases/0-mvp/index.json`
- `backend/`

이전 step의 답변 공개 규칙과 커플 소유권 검증 방식을 재사용하라.

## 작업

기록 목록과 주문/export backend API를 구현한다.

API 계약:
- `GET /api/diary`
  - 날짜 내림차순 기록 목록을 반환한다.
  - 잠금 상태에서는 상대 답변 content를 응답하지 않는다.
  - `exportable`은 둘 다 답한 기록에만 true다.
- `POST /api/exports`
  - request: `dailyQuestionIds`
  - 하나 이상의 현재 사용자 커플 기록만 주문 신청으로 만든다.
  - 둘 다 답한 기록만 허용한다.
- `POST /api/exports/{exportRequestId}/preview`
  - 접근 가능한 공개 답변만 포함한 미리보기를 만들고 상태를 PREVIEWED로 바꾼다.
- `POST /api/exports/{exportRequestId}/complete`
  - PREVIEWED 주문을 COMPLETED로 바꾸고 JSON/text payload 스냅샷을 DB에 저장한다.
- `POST /api/exports/{exportRequestId}/cancel`
  - REQUESTED 또는 PREVIEWED 주문만 CANCELLED로 바꾼다.
- `GET /api/exports/{exportRequestId}/download?format=json`
  - COMPLETED 주문의 저장된 JSON payload를 attachment로 반환한다.
- `GET /api/exports/{exportRequestId}/download?format=text`
  - COMPLETED 주문의 저장된 text payload를 attachment로 반환한다.

구현 요구:
- 주문 상태는 REQUESTED, PREVIEWED, COMPLETED, CANCELLED만 사용한다.
- COMPLETED 전에는 다운로드를 거부한다.
- 완료 후 원본 답변이 수정되어도 다운로드 payload가 바뀌지 않도록 DB 스냅샷을 사용한다.
- JSON/text entries는 날짜 오름차순으로 생성한다.
- 파일명은 `couple-diary-{exportRequestId}.json`과 `couple-diary-{exportRequestId}.txt`를 사용한다.
- 모든 API는 현재 세션 사용자와 커플 소유권을 검증한다.

테스트:
- 기록 목록 날짜 내림차순과 잠금 content 비노출을 검증한다.
- 주문 신청의 빈 항목, 접근 불가 항목, 잠금 항목 실패를 검증한다.
- REQUESTED -> PREVIEWED -> COMPLETED 상태 전이를 검증한다.
- 완료 전 다운로드 거부와 완료 후 JSON/text 다운로드를 검증한다.
- 완료 후 답변 수정에도 다운로드 결과가 유지되는지 검증한다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. 주문 완료 전 다운로드가 불가능한지 확인한다.
3. export payload 스냅샷이 개인정보로 취급되고 로그에 남지 않는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 5를 `"completed"`로 바꾸고 `summary`에 기록과 export API 산출물을 한 줄로 요약한다.

## 금지 사항

- 결제, 배송, PDF, 실물 인쇄 주문을 구현하지 마라. 이유: MVP 제외 사항이다.
- 주문 완료 후 payload를 실시간 답변 조회로 다시 만들지 마라. 이유: 스냅샷 불변 규칙 위반이다.
- frontend 파일을 수정하지 마라. 이유: 이 step은 backend API 구현만 담당한다.
