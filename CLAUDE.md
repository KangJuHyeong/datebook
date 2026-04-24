# CLAUDE.md

## 프로젝트 개요
하루 한 질문 교환일기는 Next.js 프론트엔드, Spring Boot 백엔드, MySQL 데이터베이스로 구성된 로컬 MVP 프로젝트다. 핵심 흐름은 오늘 질문, 답변 동시 공개, 기록 열람, 주문 미리보기, JSON/text 다운로드다.

## 로컬 실행

### 데이터베이스
```bash
docker compose up -d
```

### 백엔드
```bash
cd backend
./gradlew run
```

Windows PowerShell:
```powershell
cd backend
.\gradlew.bat run
```

### 프론트엔드
```bash
cd frontend
npm install
npm run dev
```

## 환경 파일
- 프론트엔드 로컬 환경 파일: `frontend/.env.local`
- 프론트엔드 예시 파일: `frontend/.env.example`
- 백엔드 로컬 환경 파일: `backend/.env`
- 백엔드 예시 파일: `backend/.env.example`

## 기본 로컬 값
- 프론트엔드 API base URL: `http://localhost:8080`
- 백엔드 서버 포트: `8080`
- 백엔드 DB 포트: `3307`
- CORS 허용 origin: `http://localhost:3000`

## 구현 가드레일
- 비즈니스 로직과 권한 검증은 백엔드 service 계층에 둔다.
- 프론트엔드는 라우팅, 렌더링, API 연동에 집중한다.
- 상대 답변 content는 두 사람이 모두 답하기 전까지 내려주지 않는다.
- 사용자-facing 문구에서는 `export` 대신 `주문`을 사용한다.
- 완료된 주문 결과는 스냅샷으로 유지한다.

## 현재 API 범위
- 인증: 회원가입, 로그인, 현재 사용자 조회, 로그아웃
- 커플: 생성, 초대 코드 참여
- 질문: 오늘 질문 조회
- 답변: 생성, 수정
- 기록: 목록 조회
- 주문: 생성, 미리보기, 완료, 취소, 다운로드

## 참고 파일
- 질문 seed: `backend/src/main/resources/data.sql`
- 백엔드 설정: `backend/src/main/resources/application.yml`
- 프론트 API base URL 설정: `frontend/src/lib/api/runtime.mjs`
