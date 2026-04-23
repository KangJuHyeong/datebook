# Step 7: frontend-auth-couple

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/UI_GUIDE.md`
- `phases/0-mvp/index.json`
- `frontend/`

이전 frontend 기반과 backend auth/couple API 계약을 확인한 뒤 작업하라.

## 작업

회원가입, 로그인, 로그아웃 진입점, 커플 연결 화면을 구현한다.

화면:
- `/signup`
  - 이메일, 비밀번호, 표시 이름 입력을 제공한다.
  - 필드별 validation 오류를 입력 아래에 표시한다.
  - 가입 성공 후 현재 사용자 상태에 따라 `/couple` 또는 `/today`로 이동한다.
- `/login`
  - 이메일, 비밀번호 입력을 제공한다.
  - 로그인 실패 시 이메일 존재 여부를 노출하지 않는 일반 오류를 표시한다.
  - 로그인 성공 후 현재 사용자 상태에 따라 `/couple` 또는 `/today`로 이동한다.
- `/couple`
  - 로그인했지만 커플이 없는 사용자가 초대 코드 생성 또는 초대 코드 입력을 할 수 있게 한다.
  - 초대 코드 생성 후 코드, 만료 시간, 복사 버튼, 상대 참여 대기 상태를 보여준다.
  - 초대 코드 참여 성공 후 `/today`로 이동한다.

UX 요구:
- UI_GUIDE의 stone/rose 색상, `rounded-lg` 카드, `rounded-md` 버튼 규칙을 따른다.
- 모든 입력 필드는 시각적 label과 programmatic label을 가진다.
- API 요청 중 버튼은 disabled 처리한다.
- 사용자-facing 문구에서 `export` 단어를 쓰지 않는다.
- 과한 히어로, gradient-text, glass morphism, glow 장식을 사용하지 않는다.

테스트:
- 로그인/회원가입 폼 validation과 성공/실패 흐름을 테스트한다.
- 커플 초대 코드 생성과 참여 흐름을 API mock 기반으로 테스트한다.
- 미로그인 사용자의 `/couple` 접근 처리와 이미 로그인한 사용자의 `/login` 접근 처리를 테스트한다.

## Acceptance Criteria

```bash
cd frontend && npm run build
cd frontend && npm run lint
cd frontend && npm run test
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. 모바일에서 주요 버튼과 입력 영역이 터치하기 충분한지 CSS를 확인한다.
3. 에러 메시지가 필드와 `aria-describedby`로 연결되는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 7을 `"completed"`로 바꾸고 `summary`에 auth/couple UI 산출물을 한 줄로 요약한다.

## 금지 사항

- backend 파일을 수정하지 마라. 이유: frontend auth/couple 구현만 담당한다.
- 오늘 질문, 기록, 주문 화면을 구현하지 마라. 이유: 이후 step의 책임이다.
- 소셜 로그인/OAuth를 추가하지 마라. 이유: MVP 제외 사항이다.
