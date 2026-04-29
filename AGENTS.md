# 프로젝트: 하루 한 질문 교환일기

## 기술 스택
- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS
- Backend: Spring Boot, Java, Spring Data JPA
- Database: MySQL
- Auth: 이메일/비밀번호 로그인 + Spring 서버 세션
- Query: 기본은 Spring Data JPA, 복잡한 동적 조회가 필요할 때만 QueryDSL 도입
- Runtime: 로컬 개발 환경 기준. 별도 배포는 MVP 범위에서 제외

## 아키텍처 규칙
- CRITICAL: Next.js는 UI와 Spring Boot API client/BFF 역할만 맡는다. 비즈니스 로직은 Spring Boot API에 둔다.
- CRITICAL: 인증은 세션 쿠키 기반으로 처리한다. 프론트엔드 fetch 요청은 credentials 포함을 기본으로 한다.
- CRITICAL: 런타임 AI 질문 생성은 MVP에서 구현하지 않는다. 초기 질문은 seed 데이터로 MySQL에 저장한다.
- CRITICAL: 답변 공개는 동시 공개 규칙을 따른다. 상대가 아직 답하지 않았으면 상대 답변 내용은 절대 내려주지 않는다.
- CRITICAL: 주문은 주문 신청, 주문 미리보기, 주문 완료, 다운로드 순서로만 진행한다. 주문 완료 전에는 다운로드를 제공하지 않는다.
- CRITICAL: 주문 완료 시 JSON/text payload를 DB 스냅샷으로 저장한다. 완료 후 원본 답변이 수정되어도 기존 주문 다운로드 내용은 바뀌지 않아야 한다.
- CRITICAL: 주문 내역은 현재 세션 사용자가 속한 커플의 주문만 조회할 수 있다. 진행 중인 미리보기 주문은 재개할 수 있고, 완료된 주문은 저장된 스냅샷만 다운로드한다.
- CRITICAL: 백엔드 API 오류는 전역 예외 처리기에서 공통 JSON 형식으로 반환한다. controller에서 try/catch로 개별 오류 응답을 만들지 않는다.
- CRITICAL: 사용자-facing UI 문구에서는 export 대신 주문, 기록 선택, 다운로드 용어를 사용한다. API, 코드, 파일명에서는 export 용어를 유지할 수 있다.
- CRITICAL: 답변, 주문, 잠금, 오류 상태는 색상만으로 표현하지 않는다. 배지 텍스트, 안내 문구, aria 속성을 함께 제공한다.
- Frontend 화면 전용 UI는 우선 `app/{route}/{route}-client.tsx`에 route-local Client Component로 둔다.
- 재사용 UI 컴포넌트는 components/ 아래에 두고, features/는 테스트 가능한 순수 로직 또는 여러 화면에서 공유되는 기능에 사용한다.
- features/ 아래에 새 화면 panel/container를 만들지 않는다.
- 브라우저 API 호출 래퍼는 lib/api/에 둔다.
- Next.js BFF route handler는 app/api/ 아래에 엔드포인트별로 두고, Spring Boot 전달 공통 로직은 lib/server/에 둔다. BFF에는 도메인 판단 로직을 넣지 않는다.
- Backend 계층은 controller, service, repository, domain, dto, config로 분리한다.
- 다른 커플의 질문, 답변, export 데이터에 접근할 수 없도록 모든 보호 API에서 현재 세션 사용자와 커플 소유권을 검증한다.

## 개발 프로세스
- CRITICAL: 새 기능 구현 시 테스트 가능한 단위부터 설계하고, 핵심 서비스 로직은 테스트를 우선 작성한다.
- CRITICAL: Harness phase step은 하나의 레이어 또는 모듈만 다룬다. 프론트엔드, 백엔드, DB 마이그레이션을 한 step에 섞지 않는다.
- CRITICAL: Harness step 세션은 `phases/{task-name}/index.json`의 해당 step status와 summary/error_message/blocked_reason만 갱신한다. 커밋은 `scripts/execute.py`가 자동으로 수행한다.
- CRITICAL: Harness 실행은 `feat-{task-name}` 브랜치에서 진행한다. step 세션은 직접 git commit/push를 실행하지 않는다.
- Harness 커밋은 `scripts/execute.py`가 자동 수행하며, 코드 변경은 `feat(...)`, phase 메타데이터는 `chore(...)` 커밋으로 분리한다.
- `phases/**/step*-output.json`은 Harness 실행 로그이므로 커밋 대상이 아니다.
- 커밋 메시지는 conventional commits 형식을 따른다. 예: feat:, fix:, docs:, refactor:
- 문서와 구현이 충돌하면 문서를 먼저 업데이트한 뒤 구현한다.
- MVP에서는 결제, 배송, PDF 생성, 실물 인쇄 주문, 소셜 로그인, 모바일 앱을 구현하지 않는다.

## 환경 파일
- Frontend 로컬 환경 파일: `frontend/.env.local`
- Frontend 예시 파일: `frontend/.env.example`
- Backend 로컬 환경 파일: `backend/.env`
- Backend 예시 파일: `backend/.env.example`
- Backend 기본 DB 포트는 로컬 MySQL 충돌을 피하기 위해 `3307`을 사용한다.

## 명령어
### Frontend
```bash
cd frontend && npm run dev      # Next.js 개발 서버, 기본 http://localhost:3000
cd frontend && npm run build    # 프론트엔드 프로덕션 빌드
cd frontend && npm run lint     # ESLint
cd frontend && npm run test     # 프론트엔드 테스트
```

### Backend
```bash
cd backend && ./gradlew run     # Spring Boot API 서버, 기본 http://localhost:8080
cd backend && ./gradlew test    # 백엔드 테스트
cd backend && ./gradlew build   # 백엔드 빌드
```

### Windows PowerShell Backend
```powershell
cd backend
.\gradlew.bat run
```

### Database
```bash
docker compose up -d mysql    # 로컬 MySQL 실행
docker compose down           # 로컬 MySQL 종료
```

### Harness
```bash
python scripts/execute.py {task-name}         # Codex CLI로 phase 순차 실행
python scripts/execute.py {task-name} --push  # Codex 실행 후 브랜치 push
python -m pytest scripts/test_execute.py      # harness executor 테스트
```
