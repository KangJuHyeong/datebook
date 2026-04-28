# 하루 한 질문 교환일기 (Datebook)

커플이 매일 하나의 질문에 각자 답하고, 둘 다 답한 뒤 서로의 답변을 열람하며, 원하는 기록을 묶어 주문하고 JSON/text 데이터로 다운로드할 수 있는 교환일기 서비스입니다.

## 1. 서비스 소개

### 대상 사용자

- 매일 짧은 대화 루틴을 만들고 싶은 커플
- 장거리 연애처럼 서로의 하루를 기록으로 남기고 싶은 사용자
- 기념일 편지, 포토북, 책 제작 등으로 확장할 수 있는 관계 기록 데이터를 쌓고 싶은 사용자

### 주요 기능

- 이메일/비밀번호 기반 회원가입, 로그인, 로그아웃
- 초대 코드 생성 및 참여를 통한 커플 연결
- Asia/Seoul 날짜 기준 오늘의 질문 제공
- 내 답변 작성 및 수정
- 둘 다 답변해야 서로의 답변이 열리는 동시 공개
- 날짜별 기록 목록 조회
- 공개 완료된 기록 선택 후 주문 신청
- 주문 미리보기, 주문 완료, JSON/text 다운로드
- 완료된 주문 데이터의 DB 스냅샷 저장

## 2. 실행 방법

### 요구 사항

- Docker 또는 Docker Desktop

### 저장소 클론

```bash
git clone <repo-url>
cd datebook
```

### 전체 서비스 실행

```bash
docker compose up --build
```

브라우저에서 아래 주소로 접속합니다.

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- MySQL: `localhost:3307`

실행 후 회원가입을 하고, 한 사용자는 초대 코드를 만들고 다른 사용자는 그 코드로 참여하면 오늘 질문과 답변 흐름을 확인할 수 있습니다.

Docker Compose 실행에는 별도 `.env` 파일이 필수는 아닙니다. `docker-compose.yml`에 기본값이 들어 있고, 백엔드 컨테이너에는 `DB_HOST=mysql`, `DB_NAME=couple_diary`, `DB_USERNAME=couple_diary` 같은 DB 접속 정보가 환경변수로 주입됩니다.

원하면 루트 예시 파일을 복사해서 포트만 관리할 수 있습니다.

```bash
cp .env.example .env
docker compose up --build
```

### 포트 변경

심사자 환경에서 포트가 겹치면 환경변수로 바꿀 수 있습니다.

```bash
FRONTEND_PORT=4000 BACKEND_PORT=18080 MYSQL_PORT=13307 docker compose up --build
```

Windows PowerShell:

```powershell
$env:FRONTEND_PORT=4000
$env:BACKEND_PORT=18080
$env:MYSQL_PORT=13307
docker compose up --build
```

포트를 바꾼 경우 접속 주소도 `http://localhost:<FRONTEND_PORT>`로 변경됩니다.

### 종료

```bash
docker compose down
```

### 더미 데이터

`backend/src/main/resources/data.sql`에 오늘 질문으로 사용할 seed 질문 데이터가 포함되어 있습니다. 별도 데이터 입력 없이 회원가입과 커플 연결만 진행하면 seed 질문을 기반으로 서비스를 바로 확인할 수 있습니다.

### 로컬 개발 실행

Docker Compose는 과제 확인용 기본 실행 경로입니다. 로컬에서 개별 실행하려면 아래 예시 환경 파일을 복사한 뒤 DB, 백엔드, 프론트엔드를 각각 실행할 수 있습니다.

```bash
cp frontend/.env.example frontend/.env.local
cp backend/.env.example backend/.env
docker compose up -d mysql
cd backend && ./gradlew run
cd frontend && npm install && npm run dev
```

## 3. 완성한 레벨

### Lv1 서비스 구현

구현 완료.

- 회원가입, 로그인, 로그아웃, 현재 사용자 조회
- 초대 코드 기반 커플 생성 및 참여
- 오늘 질문 조회
- 답변 작성 및 수정
- 둘 다 답변한 뒤 공개되는 동시 공개 규칙
- 날짜별 기록 조회

### Lv2 자체 주문 기능

구현 완료.

- 공개 완료된 기록만 주문 대상으로 선택
- 주문 신청 생성
- 주문 상태 관리: `REQUESTED`, `PREVIEWED`, `COMPLETED`, `CANCELLED`
- 주문 미리보기 생성
- 주문 완료 처리
- 주문 완료 전 다운로드 제한

### Lv3 주문 데이터 익스포트

구현 완료.

- 주문 완료 시 JSON/text payload를 DB에 스냅샷으로 저장
- 완료된 주문의 JSON 다운로드
- 완료된 주문의 text 다운로드
- 원본 답변이 수정되어도 기존 주문 다운로드 내용이 바뀌지 않도록 설계

## 4. 기술 스택 및 아키텍처

### 기술 스택

- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS
- Backend: Spring Boot, Java, Spring Data JPA
- Database: MySQL
- Auth: 이메일/비밀번호 로그인, Spring 서버 세션
- Runtime: 로컬 개발 환경, Docker Compose MySQL

### 스택 선택 이유

Next.js는 화면 라우팅과 사용자 인터랙션을 빠르게 구현하기 좋고, Spring Boot와 Spring Data JPA는 사용자, 커플, 질문, 답변, 주문처럼 관계가 분명한 도메인 로직을 안정적으로 표현하기 좋다고 판단했습니다. MySQL은 주문 스냅샷과 기록 데이터를 관계형 모델로 저장하기에 적합하며, 추후 실제 인쇄 API 연동이나 관리자 기능으로 확장하기 쉽습니다.

### 아키텍처 원칙

- Next.js는 UI와 Spring Boot API client/BFF 역할만 담당합니다.
- 비즈니스 로직, 권한 검증, 답변 공개 규칙은 Spring Boot service 계층에 둡니다.
- 인증은 세션 쿠키 기반으로 처리합니다.
- 잠긴 상대 답변 content는 서버 응답에 포함하지 않습니다.
- 완료된 주문의 JSON/text 결과는 DB 스냅샷으로 저장합니다.

### 주요 디렉터리

```text
frontend/
  src/app/          Next.js App Router 화면과 BFF route handler
  src/components/   재사용 UI 컴포넌트
  src/features/     테스트 가능한 순수 로직
  src/lib/api/      브라우저 API 호출 래퍼
  src/lib/server/   Spring Boot 전달 helper

backend/
  src/main/java/app/controller/  REST API controller
  src/main/java/app/service/     핵심 비즈니스 로직
  src/main/java/app/repository/  Spring Data JPA repository
  src/main/java/app/domain/      JPA entity
  src/main/java/app/dto/         request/response DTO
  src/main/java/app/common/      error, time 등 공통 코드
```

### 주요 API 범위

- Auth: `/api/auth/signup`, `/api/auth/login`, `/api/auth/me`, `/api/auth/logout`
- Couple: `/api/couples`, `/api/couples/join`
- Question: `/api/questions/today`
- Answer: `/api/answers`, `/api/answers/{answerId}`
- Diary: `/api/diary`
- Export Order: `/api/exports`, `/api/exports/{id}/preview`, `/api/exports/{id}/complete`, `/api/exports/{id}/cancel`, `/api/exports/{id}/download`

## 5. AI 도구 사용 내역

| AI 도구 | 활용 내용 |
| --- | --- |
| Codex | 프로젝트 문서 정리, README 작성, 구현 구조 점검 |
| ChatGPT / Claude 계열 도구 | 서비스 아이디어 구체화, PRD/아키텍처 초안 작성, 테스트 케이스 관점 정리 |
| AI 코드 작성 도구 | Spring Boot service/controller, Next.js 화면, BFF route handler, 테스트 코드 작성 보조 |

AI 도구는 전체 방향을 대신 결정하게 하기보다, 요구사항을 쪼개고 반복 구현 속도를 높이는 방식으로 사용했습니다. 특히 답변 동시 공개, 주문 상태 전이, 주문 데이터 스냅샷처럼 실수하면 서비스 신뢰에 영향을 주는 부분은 문서화된 규칙과 테스트를 기준으로 확인했습니다.

## 6. 설계 의도

### 아이디어 선택 이유

인쇄 API를 활용할 수 있는 서비스라면 책 제작 자체보다 먼저 꾸준히 쌓이는 콘텐츠가 있어야 한다고 생각했습니다. 그래서 사용자가 매일 돌아올 이유가 있는 교환일기를 본체로 두고, 쌓인 기록을 나중에 책이나 앨범으로 만들 수 있는 부가 흐름으로 주문 기능을 설계했습니다.

### 사업적 가능성

커플의 기록은 감정적 가치가 크고, 기념일·생일·연말 같은 특정 시점에 실제 책, 카드, 포토북으로 전환될 가능성이 있습니다. MVP에서는 JSON/text 다운로드까지만 구현했지만, 이후에는 스위트북 같은 인쇄 API와 연동해 선택한 기록을 실제 책 제작 주문으로 넘길 수 있습니다.

### 더 시간이 있었다면 추가할 기능

- 주문 결과 미리보기 화면 고도화
- 실제 인쇄 API 연동용 export schema 정교화
- 이미지 첨부와 사진 기록
- 질문 카테고리와 사용자 직접 질문 추가
- 커플별 기념일 기반 질문 추천
- E2E 테스트와 Docker Compose 전체 앱 실행 구성

## 7. 테스트

### Frontend

```bash
cd frontend
npm run test
```

### Backend

```bash
cd backend
./gradlew test
```

Windows PowerShell:

```powershell
cd backend
.\gradlew.bat test
```

## 8. 참고 문서

- 프로젝트 규칙: `AGENTS.md`
- 제품 요구사항: `docs/PRD.md`
- 아키텍처: `docs/ARCHITECTURE.md`
- 의사결정 기록: `docs/ADR.md`
- UI 가이드: `docs/UI_GUIDE.md`
