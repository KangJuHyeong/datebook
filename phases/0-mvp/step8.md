# Step 8: frontend-today-diary

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/UI_GUIDE.md`
- `phases/0-mvp/index.json`
- `frontend/`

이전 frontend auth/couple 화면과 API client를 읽고 일관성을 유지하라.

## 작업

오늘 질문 화면과 기록 목록 화면을 구현한다.

화면:
- `/today`
  - 오늘의 질문, 내 답변 작성/수정 폼, 상대 답변 잠금/공개 상태를 보여준다.
  - 답변 저장은 수동 저장이다.
  - 답변이 비어 있거나 2000자를 초과하면 저장 버튼을 비활성화한다.
  - 저장 요청 중 버튼을 비활성화해 중복 제출을 막는다.
  - 저장 성공 후 짧은 상태 피드백을 보여준다.
  - 저장하지 않은 변경이 있을 때 페이지 이탈 경고 또는 앱 내부 확인 UI를 제공한다.
- `/diary`
  - 날짜 내림차순 기록 목록을 보여준다.
  - 각 기록은 날짜, 질문, 내 답변 상태, 상대 답변 공개 여부를 포함한다.
  - 둘 다 답한 기록은 양쪽 답변을 읽을 수 있게 하고, 잠금 기록은 답변 내용을 추측할 수 없는 잠금 상태로 표시한다.
  - 기록이 없으면 오늘 질문으로 이동하는 primary action을 제공한다.

UX 요구:
- 상태는 색상만으로 표현하지 않고 배지 텍스트와 안내 문구를 함께 제공한다.
- PARTNER_ANSWERED_ME_WAITING 문구는 "상대가 답변을 마쳤어요. 내 답변을 남기면 함께 열려요."를 기본으로 한다.
- MY_ANSWERED_PARTNER_WAITING 문구는 "내 답변은 저장됐어요. 상대가 답하면 함께 열려요."를 기본으로 한다.
- 오늘 질문 로딩과 기록 목록 로딩은 레이아웃 높이를 크게 흔들지 않게 처리한다.
- 마케팅 랜딩이 아니라 앱 화면을 첫 경험으로 유지한다.

테스트:
- 오늘 질문 상태 4종 렌더링을 테스트한다.
- 잠금 상태에서 상대 답변 content를 표시하지 않는지 테스트한다.
- 답변 저장 validation, 중복 제출 방지, 성공/실패 피드백을 테스트한다.
- 기록 목록 빈 상태와 공개/잠금 항목 렌더링을 테스트한다.

## Acceptance Criteria

```bash
cd frontend && npm run build
cd frontend && npm run lint
cd frontend && npm run test
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. 잠금 상태에서 상대 답변 내용을 렌더링하지 않는지 확인한다.
3. UI_GUIDE의 문구와 레이아웃 규칙을 따르는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 8을 `"completed"`로 바꾸고 `summary`에 today/diary UI 산출물을 한 줄로 요약한다.

## 금지 사항

- backend 파일을 수정하지 마라. 이유: frontend today/diary 구현만 담당한다.
- 주문/다운로드 화면을 구현하지 마라. 이유: step 9의 책임이다.
- 자동 저장을 구현하지 마라. 이유: PRD에서 MVP는 수동 저장으로 정했다.
