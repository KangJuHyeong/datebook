# Step 0: project-scaffold

## 읽어야 하는 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ADR.md`
- `docs/UI_GUIDE.md`

## 작업

MVP 구현을 시작할 수 있도록 저장소의 기본 구조만 만든다. 이 step에서는 애플리케이션 기능 구현을 하지 말고, 이후 step들이 작업할 수 있는 빌드 가능한 골격을 만든다.

Backend:
- `backend/`에 Spring Boot Java 프로젝트를 구성한다.
- Java 17 이상, Spring Boot, Spring Web, Spring Data JPA, Spring Validation, Spring Security, MySQL driver, test 의존성을 사용한다.
- 패키지 루트는 `app`으로 한다.
- `app` 아래에 `config`, `controller`, `service`, `repository`, `domain`, `dto`, `common` 패키지를 만든다.
- `common/error` 패키지에는 이후 step에서 확장할 수 있는 최소 오류 응답 골격을 둔다.
- `application.yml`은 로컬 MySQL 기준으로 작성하되, 테스트는 인메모리 DB 또는 테스트 프로파일로 독립 실행 가능하게 한다.

Frontend:
- `frontend/`에 Next.js App Router, React, TypeScript, Tailwind CSS 프로젝트를 구성한다.
- `src/app`, `src/components`, `src/features`, `src/lib/api`, `src/types` 구조를 만든다.
- 루트 페이지는 현재 사용자 상태 확인 전까지 최소 placeholder만 제공하고, 실제 화면 구현은 이후 step에 맡긴다.
- Tailwind 기본 테마는 `docs/UI_GUIDE.md`의 stone/rose 중심 규칙을 따를 수 있게 준비한다.

Repository:
- 루트에 `docker-compose.yml`을 추가해 MySQL 8 로컬 개발 컨테이너를 제공한다.
- 필요한 경우 `.gitignore`에 빌드 산출물, 환경 파일, DB 볼륨, generated 파일을 추가한다.
- 기존 Harness 파일과 문서는 수정하지 않는다.

## Acceptance Criteria

```bash
cd backend && ./gradlew test
cd frontend && npm run build
cd frontend && npm run lint
python -m pytest scripts/test_execute.py
```

## 검증 절차

1. AC 커맨드를 실행한다.
2. `backend/`와 `frontend/`가 각각 독립적으로 빌드 가능한지 확인한다.
3. ARCHITECTURE.md의 디렉터리 구조와 AGENTS.md의 레이어 분리 규칙을 위반하지 않았는지 확인한다.
4. 성공하면 `phases/0-mvp/index.json`의 step 0을 `"completed"`로 바꾸고 `summary`에 생성된 골격을 한 줄로 요약한다.
5. 3회 수정 후에도 실패하면 `"error"`와 구체적인 `error_message`를 기록한다.

## 금지 사항

- 도메인 기능을 미리 구현하지 마라. 이유: 이후 step의 책임 범위를 침범한다.
- Next.js API Route에 비즈니스 로직을 만들지 마라. 이유: Spring Boot가 도메인 로직을 담당한다.
- 런타임 AI 호출이나 외부 AI 의존성을 추가하지 마라. 이유: MVP 범위 밖이다.
