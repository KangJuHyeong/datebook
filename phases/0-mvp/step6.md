# Step 6: frontend-app-shell-api

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ADR.md`
- `docs/UI_GUIDE.md`
- `phases/0-mvp/index.json`
- `frontend/`
- `backend/src/main/java/app/controller/`
- `backend/src/main/java/app/dto/`

이전 backend step에서 확정된 API 응답 DTO와 오류 형식을 확인한 뒤 작업하라.

## 작업

Frontend 앱 셸, API client, 타입, 라우팅 가드 기반을 구현한다. 개별 화면의 상세 폼과 기능 UI는 이후 step으로 넘긴다.

구현 범위:
- `frontend/src/lib/api/`에 Spring Boot API fetch wrapper를 만든다.
- 모든 보호 API 요청은 `credentials: "include"`를 기본으로 사용한다.
- 공통 오류 응답 `{ code, message, fields }`를 TypeScript 타입으로 정의한다.
- auth, couple, today, diary, export API 함수의 인터페이스를 backend 계약에 맞춰 만든다.
- 실제 UI 화면에서 재사용할 `AppLayout`, navigation, status badge, button/input 같은 최소 공통 컴포넌트를 만든다.
- 루트(`/`)는 `GET /api/auth/me` 결과에 따라 `/login`, `/couple`, `/today` 중 하나로 이동한다.
- `/today`, `/diary`, `/export` 보호 라우트에서 미로그인 사용자는 `/login`, 커플 없는 사용자는 `/couple`로 이동할 수 있는 guard 기반을 만든다.
- 사용자-facing UI 문구에서 `export` 단어를 쓰지 않는 규칙을 타입/상수/컴포넌트 이름과 화면 문구에서 구분한다. 코드 내부 API명에는 export를 사용할 수 있다.

테스트:
- API client가 credentials를 포함하는지 검증한다.
- 오류 응답 파싱과 401/409 처리 경로를 검증한다.
- 루트 라우팅 결정 또는 guard 유틸을 테스트한다.

## Acceptance Criteria

```bash
cd frontend && npm run build
cd frontend && npm run lint
cd frontend && npm run test
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. Next.js API Route에 비즈니스 로직이 들어가지 않았는지 확인한다.
3. API client가 credentials 포함 규칙을 지키는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 6을 `"completed"`로 바꾸고 `summary`에 frontend 기반 산출물을 한 줄로 요약한다.

## 금지 사항

- 백엔드 파일을 수정하지 마라. 이유: 이 step은 frontend 기반만 담당한다.
- 실제 auth/couple/today/diary/export 화면의 상세 UX를 완성하지 마라. 이유: 이후 step의 책임이다.
- 사용자-facing 문구에 export를 노출하지 마라. 이유: UI_GUIDE와 AGENTS.md 규칙 위반이다.
