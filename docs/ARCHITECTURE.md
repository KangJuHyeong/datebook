# 아키텍처

## 개요
하루 한 질문 교환일기는 Next.js 프론트엔드와 Spring Boot 백엔드를 분리한 로컬 MVP 웹 애플리케이션이다. Next.js는 화면 렌더링과 얇은 BFF/API client 역할을 맡고, Spring Boot는 인증, 도메인 로직, 데이터 접근, 주문/export 생성을 담당한다.

## 시스템 구성
- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS
- Backend: Spring Boot, Java, Spring Data JPA
- Database: MySQL
- Auth: 이메일/비밀번호 + Spring 서버 세션
- Runtime: 로컬 실행, 환경 변수 파일 기반 설정
- Timezone: Asia/Seoul
- Timestamp storage: DB의 DATETIME은 UTC 기준으로 저장하고, 질문 날짜만 Asia/Seoul LocalDate로 계산한다.

## 디렉토리 구조
### Frontend
```text
frontend/
├── .env.local                # 로컬 프론트엔드 설정 (gitignore)
├── .env.example              # 프론트엔드 환경 변수 예시
└── src/
    ├── app/                  # App Router 페이지, 레이아웃, BFF route handler
    │   ├── api/              # endpoint별 Next.js BFF route handler
    │   ├── login/
    │   ├── signup/
    │   ├── couple/
    │   ├── today/
    │   ├── diary/
    │   └── export/
    ├── components/           # 재사용 UI 컴포넌트
    ├── features/             # 테스트 가능한 순수 로직 또는 여러 화면 공유 기능
    ├── lib/
    │   ├── api/              # 브라우저/서버 공용 API fetch wrapper
    │   └── server/           # BFF에서 Spring Boot로 전달하는 서버 전용 helper
    └── types/                # TypeScript 타입 정의
```

### Backend
```text
backend/
├── .env                      # 로컬 백엔드 설정 (gitignore)
├── .env.example              # 백엔드 환경 변수 예시
└── src/main/java/
    └── app/
        ├── config/           # security, cors, session, web 설정
        ├── controller/       # REST API endpoint
        ├── service/          # 비즈니스 로직
        ├── repository/       # Spring Data JPA repository
        ├── domain/           # JPA entity
        ├── dto/              # request/response DTO
        └── common/           # error, time, response 공통 코드
└── src/main/resources/
    ├── application.yml       # env import, datasource, CORS, server 설정
    └── data.sql              # 질문 seed 데이터
```

## 패턴
- Frontend는 페이지 단위 라우팅을 App Router로 구성한다.
- `app/{route}/page.tsx`는 Server Component로 인증 가드, 라우팅 경계, 레이아웃 조립을 담당한다.
- 인터랙션이 필요한 폼, 체크 선택, 답변 작성 UI는 `app/{route}/{route}-client.tsx` 같은 route-local Client Component로 만든다.
- `features/**`는 화면 전용 panel/container가 아니라 테스트 가능한 순수 로직 또는 여러 화면에서 공유되는 기능에 사용한다.
- 화면 전용 UI는 route가 소유하며, panel/container 계층은 필수 패턴으로 두지 않는다.
- Next.js BFF route handler는 app/api/ 아래에 엔드포인트별로 두고, Spring Boot API로 요청을 전달한다.
- Next.js BFF route handler에 도메인 로직을 넣지 않는다.
- Spring Boot controller는 요청/응답 변환만 담당하고 핵심 판단은 service에 둔다.
- service는 세션 사용자 조회, 커플 권한 검증, 상태 전이를 책임진다.
- repository는 기본적으로 Spring Data JPA를 사용한다.
- 기록 검색이나 export 선택 조건이 복잡해질 때만 QueryDSL을 도입한다.

## Frontend 라우팅 가드
- `/login`, `/signup`은 미로그인 사용자용 화면이다. 이미 로그인한 사용자는 커플 상태에 따라 `/today` 또는 `/couple`로 이동한다.
- `/couple`은 로그인했지만 커플이 없거나 상대 초대 대기 중인 사용자용 화면이다.
- `/today`, `/diary`, `/export`는 로그인과 커플 연결이 모두 필요하다.
- 미로그인 사용자가 보호 라우트에 접근하면 `/login`으로 이동한다.
- 로그인했지만 커플이 없는 사용자가 보호 라우트에 접근하면 `/couple`로 이동한다.
- 루트(`/`) 접근 시 현재 사용자 상태를 확인해 `/login`, `/couple`, `/today` 중 하나로 이동한다.

## 데이터 흐름
```text
사용자 입력
→ Next.js Client Component
→ lib/api fetch client, credentials 포함
→ Next.js app/api BFF route handler
→ lib/server backend proxy helper
→ Spring Boot REST API
→ Service에서 세션 사용자와 커플 권한 검증
→ Spring Data JPA
→ MySQL
→ DTO 응답
→ UI 업데이트
```

## 인증 흐름
```text
이메일/비밀번호 로그인 요청
→ Spring Boot가 비밀번호 해시 검증
→ 로그인 성공 시 서버 세션 생성
→ 세션에 userId 저장
→ 브라우저에 HttpOnly 세션 쿠키 저장
→ 이후 Next.js fetch 요청은 credentials: "include"로 쿠키 전송
→ Spring Boot가 세션의 userId로 현재 사용자 식별
```

세션 쿠키 정책:
- HttpOnly: true
- SameSite: Lax
- Secure: 로컬 MVP에서는 false, HTTPS 배포 시 true로 전환
- 세션 저장소: MVP는 Spring 기본 in-memory 세션 사용
- 로그아웃 시 서버 세션을 invalidate한다.

## 주요 도메인 모델
- User: 이메일, 비밀번호 해시, 표시 이름을 가진 사용자
- Couple: 두 사용자가 연결되는 커플 공간
- CoupleMember: 사용자와 커플의 연결 정보
- InviteCode: 커플 참여를 위한 초대 코드, 만료 시간, 사용 여부
- Question: 운영자가 seed로 저장한 질문 풀
- DailyQuestion: 특정 커플과 날짜에 배정된 질문
- Answer: 사용자가 특정 DailyQuestion에 작성한 답변
- ExportRequest: 사용자가 선택한 기록을 export 주문으로 만든 이력
- ExportItem: export 주문에 포함된 DailyQuestion 목록

## 데이터 모델
### users
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| display_name | VARCHAR(50) | NOT NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

### couples
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

### couple_members
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| couple_id | BIGINT | FK, NOT NULL |
| user_id | BIGINT | FK, NOT NULL |
| joined_at | DATETIME | NOT NULL |

제약:
- UNIQUE(user_id)
- UNIQUE(couple_id, user_id)
- 애플리케이션 레벨에서 couple_id당 최대 2명 검증

### invite_codes
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| couple_id | BIGINT | FK, NOT NULL |
| code | VARCHAR(32) | UNIQUE, NOT NULL |
| expires_at | DATETIME | NOT NULL |
| used_at | DATETIME | NULL |
| used_by_user_id | BIGINT | FK, NULL |
| created_at | DATETIME | NOT NULL |

### questions
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| content | VARCHAR(500) | NOT NULL |
| active | BOOLEAN | NOT NULL |
| sort_order | INT | NOT NULL |
| created_at | DATETIME | NOT NULL |

### daily_questions
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| couple_id | BIGINT | FK, NOT NULL |
| question_id | BIGINT | FK, NOT NULL |
| question_date | DATE | NOT NULL |
| created_at | DATETIME | NOT NULL |

제약:
- UNIQUE(couple_id, question_date)

### answers
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| daily_question_id | BIGINT | FK, NOT NULL |
| user_id | BIGINT | FK, NOT NULL |
| content | TEXT | NOT NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

제약:
- UNIQUE(daily_question_id, user_id)
- content는 1자 이상 2000자 이하를 service에서 검증

### export_requests
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| couple_id | BIGINT | FK, NOT NULL |
| requested_by_user_id | BIGINT | FK, NOT NULL |
| status | VARCHAR(20) | NOT NULL |
| created_at | DATETIME | NOT NULL |
| previewed_at | DATETIME | NULL |
| completed_at | DATETIME | NULL |
| cancelled_at | DATETIME | NULL |
| json_payload | MEDIUMTEXT | NULL |
| text_payload | MEDIUMTEXT | NULL |

### export_items
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| export_request_id | BIGINT | FK, NOT NULL |
| daily_question_id | BIGINT | FK, NOT NULL |
| sort_order | INT | NOT NULL |

제약:
- UNIQUE(export_request_id, daily_question_id)

## 공개 규칙
- 내 답변은 작성 후 확인 가능하다.
- 상대 답변은 상대가 답하기 전까지 잠금 상태다.
- 둘 다 답하면 양쪽 답변을 공개한다.
- 잠금 상태에서는 서버가 상대 답변 content를 응답하지 않는다.
- 모든 답변 조회, 수정, export 요청은 현재 세션 사용자가 속한 커플 데이터로 제한한다.

## 오늘 질문 상태 모델
- NOT_ANSWERED: 현재 사용자가 아직 답하지 않은 상태
- MY_ANSWERED_PARTNER_WAITING: 현재 사용자는 답했고 파트너가 아직 답하지 않은 상태
- PARTNER_ANSWERED_ME_WAITING: 파트너는 답했고 현재 사용자가 아직 답하지 않은 상태. 파트너 답변 content는 응답하지 않는다.
- BOTH_ANSWERED: 두 사용자 모두 답해 양쪽 답변 content를 응답할 수 있는 상태

파트너 답변 응답 상태:
- LOCKED: 파트너 답변이 없거나 공개 조건을 만족하지 않아 content를 응답하지 않는 상태
- ANSWERED_LOCKED: 파트너가 답변했지만 현재 사용자가 아직 답하지 않아 content를 응답하지 않는 상태
- REVEALED: 둘 다 답해 파트너 답변 content를 응답하는 상태

## 오늘 질문 배정
- 기준 날짜는 Asia/Seoul의 LocalDate다.
- GET /api/questions/today 호출 시 해당 커플과 날짜의 DailyQuestion이 있으면 그대로 반환한다.
- DailyQuestion이 없으면 active=true인 questions에서 하나를 선택해 생성한다.
- MVP의 선택 방식은 sort_order 기준 순환이다.
- 선택 공식: active 질문을 sort_order, id 오름차순으로 정렬하고, 해당 커플의 기존 DailyQuestion 개수를 active 질문 개수로 나눈 나머지 인덱스의 질문을 선택한다.
- 동시에 여러 요청이 들어와도 UNIQUE(couple_id, question_date)로 중복 생성을 막고, 충돌 시 기존 DailyQuestion을 재조회한다.
- 질문 seed가 비어 있으면 500 설정 오류를 반환한다.

## 주문/export 흐름
```text
기록 체크 선택
→ 주문 신청 생성, ExportRequest status=REQUESTED
→ ExportItem 생성
→ 주문 미리보기 생성, 접근 가능한 공개 답변만 포함
→ ExportRequest status=PREVIEWED
→ 사용자가 최종 확인
→ ExportRequest status=COMPLETED
→ JSON/text payload 스냅샷 저장
→ 저장된 payload 다운로드
```

주문 내역은 `/export` 화면의 주문 내역 탭에서 조회한다. 기본 목록은 `PREVIEWED`, `COMPLETED` 주문을 생성일 내림차순으로 보여준다. `PREVIEWED` 주문은 "주문 예약 단계"로 표시하고 기존 ExportItem 기준으로 미리보기를 다시 열어 완료 또는 취소를 이어갈 수 있다. `COMPLETED` 주문은 "주문 완료"로 표시하고 저장된 payload에서 복원한 스냅샷 상세, 다운로드, 삭제를 제공한다.

## 주문 상태 전이
- REQUESTED → PREVIEWED
- PREVIEWED → COMPLETED
- REQUESTED → CANCELLED
- PREVIEWED → CANCELLED
- COMPLETED는 최종 상태이며 취소하거나 항목을 바꾸지 않는다. 완료 주문은 주문 내역에서 삭제할 수 있다.
- CANCELLED는 최종 상태이며 다운로드할 수 없다.

## 주문 API 방향
- POST /api/exports: 선택한 dailyQuestionIds로 주문 신청을 생성한다.
- GET /api/exports: 현재 커플의 진행 중 미리보기 주문과 완료된 주문 목록을 생성일 내림차순으로 반환한다.
- GET /api/exports/{exportRequestId}: 현재 커플의 주문 상세를 반환한다. PREVIEWED는 미리보기 entries, COMPLETED는 저장된 JSON payload에서 복원한 entries와 다운로드 링크를 제공한다.
- POST /api/exports/{exportRequestId}/preview: 주문 확인 전 미리보기 데이터를 생성하고 주문 상태를 PREVIEWED로 바꾼다.
- POST /api/exports/{exportRequestId}/complete: 주문을 완료 상태로 바꾸고 JSON/text payload 스냅샷을 저장한다.
- POST /api/exports/{exportRequestId}/cancel: REQUESTED 또는 PREVIEWED 주문을 취소한다.
- DELETE /api/exports/{exportRequestId}: 현재 커플의 COMPLETED 주문을 삭제한다. PREVIEWED 주문은 cancel API를 사용한다.
- GET /api/exports/{exportRequestId}/download?format=json: 완료된 주문의 JSON 파일을 반환한다.
- GET /api/exports/{exportRequestId}/download?format=text: 완료된 주문의 text 파일을 반환한다.
- 미리보기와 다운로드는 현재 세션 사용자가 속한 커플의 주문에만 허용한다.
- 주문 목록과 상세 조회도 현재 세션 사용자가 속한 커플의 주문에만 허용한다.
- 다운로드와 삭제 API는 COMPLETED 상태의 주문에만 허용한다.

## 에러 처리 전략
### 공통 원칙
- 모든 API 오류는 동일한 JSON 구조로 응답한다.
- controller에서 try/catch로 개별 오류 응답을 만들지 않는다.
- 도메인 오류는 service 계층에서 의미 있는 커스텀 예외로 던진다.
- Spring Boot 전역 예외 처리기는 `@RestControllerAdvice`로 구현한다.
- 클라이언트에 Java exception class, stack trace, SQL error, 세션 ID를 노출하지 않는다.
- 사용자 입력 오류는 400, 인증 없음은 401, 권한 없음은 403, 리소스 없음은 404, 상태 전이 충돌은 409, 서버 설정/예상 밖 오류는 500으로 처리한다.
- 예상하지 못한 예외는 `INTERNAL_ERROR`로 감싸고 서버 로그에만 상세 원인을 남긴다.
- 로그인 실패 메시지는 이메일 존재 여부를 알 수 없도록 일반 메시지를 반환한다.

### Backend 패키지 구조
```text
backend/src/main/java/app/common/error/
├── ErrorCode.java              # 코드, HTTP status, 기본 메시지
├── ErrorResponse.java          # 공통 오류 응답 DTO
├── FieldErrorResponse.java     # validation field 오류 DTO
├── BusinessException.java      # 도메인/비즈니스 예외
└── GlobalExceptionHandler.java # @RestControllerAdvice
```

### 공통 오류 응답
```json
{
  "code": "AUTH_REQUIRED",
  "message": "로그인이 필요합니다.",
  "fields": []
}
```

Validation 오류 응답:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "입력값을 확인해주세요.",
  "fields": [
    {
      "field": "email",
      "message": "올바른 이메일 형식이 아닙니다."
    }
  ]
}
```

### 오류 코드와 HTTP status
| code | HTTP | 의미 |
|------|------|------|
| AUTH_REQUIRED | 401 | 로그인 필요 |
| LOGIN_FAILED | 401 | 이메일 또는 비밀번호 불일치 |
| FORBIDDEN | 403 | 현재 사용자에게 권한 없음 |
| NOT_FOUND | 404 | 리소스 없음 |
| VALIDATION_ERROR | 400 | 입력값 오류 |
| INVITE_CODE_INVALID | 400 | 초대 코드가 없거나 만료 또는 사용됨 |
| COUPLE_FULL | 409 | 커플 정원 초과 |
| ALREADY_IN_COUPLE | 409 | 이미 커플에 속한 사용자 |
| DAILY_QUESTION_CONFLICT | 409 | 오늘 질문 동시 생성 충돌 |
| ANSWER_LOCKED | 403 | 아직 공개되지 않은 답변 접근 |
| ANSWER_NOT_OWNED | 403 | 본인 답변이 아닌 답변 수정 시도 |
| EXPORT_ITEM_INVALID | 400 | 주문 항목이 비어 있거나 접근 불가 |
| EXPORT_STATE_INVALID | 409 | 허용되지 않은 주문 상태 전이 |
| EXPORT_NOT_COMPLETED | 409 | 주문 완료 전 다운로드 요청 |
| EXPORT_FORMAT_INVALID | 400 | 지원하지 않는 다운로드 format |
| CONFIGURATION_ERROR | 500 | 질문 seed 없음 등 서버 설정 오류 |
| INTERNAL_ERROR | 500 | 예상하지 못한 서버 오류 |

### 예외 매핑
- `BusinessException`: 예외가 가진 `ErrorCode`의 HTTP status로 응답한다.
- `MethodArgumentNotValidException`: `VALIDATION_ERROR`와 fields 배열로 응답한다.
- `MissingServletRequestParameterException`: `VALIDATION_ERROR`로 응답한다.
- `HttpMessageNotReadableException`: `VALIDATION_ERROR`로 응답한다.
- `NoHandlerFoundException` 또는 리소스 조회 실패: `NOT_FOUND`로 응답한다.
- `DataIntegrityViolationException`: 기본은 `INTERNAL_ERROR`로 처리하되, service에서 사전에 검증 가능한 unique 충돌은 `BusinessException`으로 변환한다.
- 그 외 `Exception`: `INTERNAL_ERROR`로 응답한다.

### 로깅 규칙
- 4xx 도메인 오류는 warn 이하로 기록한다.
- 5xx 오류는 error로 기록한다.
- 비밀번호, 답변 본문, export payload, 세션 ID는 로그에 남기지 않는다.
- 요청 추적이 필요하면 민감정보가 아닌 request path, error code, userId, coupleId만 남긴다.

### Frontend Error UX
- 401 `AUTH_REQUIRED`: 로그인 화면으로 이동한다.
- 401 `LOGIN_FAILED`: 로그인 폼 위에 일반 오류를 표시한다.
- 400 `VALIDATION_ERROR`: 필드별 메시지가 있으면 입력 필드 아래에 표시한다.
- 403: 접근할 수 없다는 짧은 안내를 표시하고 이전 안전한 화면으로 이동할 수 있게 한다.
- 404: 기록 또는 주문을 찾을 수 없다는 빈 상태 화면을 보여준다.
- 409: 최신 상태를 다시 불러오도록 안내하고 관련 데이터를 refetch한다.
- 500: "잠시 후 다시 시도해주세요." 메시지를 표시한다.

## API 계약

### GET /api/auth/me
Response:
```json
{
  "id": 1,
  "email": "a@example.com",
  "displayName": "민지",
  "coupleId": 10
}
```

### POST /api/auth/logout
Response:
```json
{
  "success": true
}
```

### POST /api/auth/signup
Request:
```json
{
  "email": "a@example.com",
  "password": "password123",
  "displayName": "민지"
}
```

Response:
```json
{
  "id": 1,
  "email": "a@example.com",
  "displayName": "민지"
}
```

### POST /api/auth/login
Request:
```json
{
  "email": "a@example.com",
  "password": "password123"
}
```

Response:
```json
{
  "id": 1,
  "email": "a@example.com",
  "displayName": "민지",
  "coupleId": 10
}
```

### POST /api/couples
Response:
```json
{
  "coupleId": 10,
  "inviteCode": "A1B2C3D4",
  "expiresAt": "2026-04-23T09:00:00"
}
```

### POST /api/couples/join
Request:
```json
{
  "inviteCode": "A1B2C3D4"
}
```

Response:
```json
{
  "coupleId": 10,
  "memberCount": 2
}
```

### GET /api/questions/today
Response:
```json
{
  "dailyQuestionId": 100,
  "date": "2026-04-22",
  "answerState": "MY_ANSWERED_PARTNER_WAITING",
  "question": "오늘 가장 먼저 떠오른 서로의 모습은 무엇인가요?",
  "myAnswer": {
    "id": 200,
    "content": "아침에 보내준 짧은 메시지가 생각났어.",
    "updatedAt": "2026-04-22T09:10:00"
  },
  "partnerAnswer": {
    "status": "LOCKED"
  },
  "isFullyAnswered": false
}
```

상대가 먼저 답변했지만 현재 사용자가 아직 답하지 않은 경우:
```json
{
  "dailyQuestionId": 100,
  "date": "2026-04-22",
  "answerState": "PARTNER_ANSWERED_ME_WAITING",
  "question": "오늘 가장 먼저 떠오른 서로의 모습은 무엇인가요?",
  "myAnswer": null,
  "partnerAnswer": {
    "status": "ANSWERED_LOCKED"
  },
  "isFullyAnswered": false
}
```

### POST /api/answers
Request:
```json
{
  "dailyQuestionId": 100,
  "content": "아침에 보내준 짧은 메시지가 생각났어."
}
```

Response:
```json
{
  "id": 200,
  "dailyQuestionId": 100,
  "content": "아침에 보내준 짧은 메시지가 생각났어.",
  "updatedAt": "2026-04-22T09:10:00"
}
```

### PUT /api/answers/{answerId}
Request:
```json
{
  "content": "수정한 답변 내용"
}
```

Response:
```json
{
  "id": 200,
  "dailyQuestionId": 100,
  "content": "수정한 답변 내용",
  "updatedAt": "2026-04-22T09:30:00"
}
```

### GET /api/diary
Response:
```json
{
  "entries": [
    {
      "dailyQuestionId": 100,
      "date": "2026-04-22",
      "question": "오늘 가장 먼저 떠오른 서로의 모습은 무엇인가요?",
      "myAnswerStatus": "ANSWERED",
      "partnerAnswerStatus": "REVEALED",
      "myAnswer": {
        "displayName": "민지",
        "content": "아침에 보내준 짧은 메시지가 생각났어."
      },
      "partnerAnswer": {
        "displayName": "도윤",
        "content": "퇴근길에 같이 걸었던 장면."
      },
      "exportable": true
    }
  ]
}
```

`exportable`은 현재 사용자의 커플에서 해당 DailyQuestion에 두 명 모두 답변했고 양쪽 답변이 공개 가능한 경우에만 true다.
`myAnswer`는 현재 사용자가 답변한 경우에만 포함된다. `partnerAnswer`는 동시 공개 규칙에 따라 `partnerAnswerStatus`가 `REVEALED`인 경우에만 포함되며, 잠금 상태에서는 응답에 포함하지 않는다.

### POST /api/exports
Request:
```json
{
  "dailyQuestionIds": [100, 101]
}
```

Response:
```json
{
  "exportRequestId": 300,
  "status": "REQUESTED",
  "itemCount": 2
}
```

### GET /api/exports
Response:
```json
{
  "orders": [
    {
      "exportRequestId": 300,
      "status": "PREVIEWED",
      "itemCount": 2,
      "createdAt": "2026-04-22T09:50:00",
      "previewedAt": "2026-04-22T09:51:00",
      "completedAt": null,
      "cancelledAt": null
    }
  ]
}
```

### GET /api/exports/{exportRequestId}
PREVIEWED Response:
```json
{
  "exportRequestId": 300,
  "status": "PREVIEWED",
  "itemCount": 2,
  "createdAt": "2026-04-22T09:50:00",
  "previewedAt": "2026-04-22T09:51:00",
  "completedAt": null,
  "cancelledAt": null,
  "entries": [
    {
      "date": "2026-04-22",
      "question": "오늘 가장 먼저 떠오른 서로의 모습은 무엇인가요?",
      "answers": [
        {
          "displayName": "민지",
          "content": "아침에 보내준 짧은 메시지가 생각났어."
        }
      ]
    }
  ],
  "downloads": []
}
```

COMPLETED Response:
```json
{
  "exportRequestId": 300,
  "status": "COMPLETED",
  "itemCount": 2,
  "createdAt": "2026-04-22T09:50:00",
  "previewedAt": "2026-04-22T09:51:00",
  "completedAt": "2026-04-22T10:00:00",
  "cancelledAt": null,
  "entries": [
    {
      "date": "2026-04-22",
      "question": "오늘 가장 먼저 떠오른 서로의 모습은 무엇인가요?",
      "answers": [
        {
          "displayName": "민지",
          "content": "아침에 보내준 짧은 메시지가 생각났어."
        },
        {
          "displayName": "도윤",
          "content": "퇴근길에 같이 걷던 장면."
        }
      ]
    }
  ],
  "downloads": [
    {
      "format": "json",
      "url": "/api/exports/300/download?format=json"
    },
    {
      "format": "text",
      "url": "/api/exports/300/download?format=text"
    }
  ]
}
```

### POST /api/exports/{exportRequestId}/preview
Response:
```json
{
  "exportRequestId": 300,
  "status": "PREVIEWED",
  "entries": [
    {
      "date": "2026-04-22",
      "question": "오늘 가장 먼저 떠오른 서로의 모습은 무엇인가요?",
      "answers": [
        {
          "displayName": "민지",
          "content": "아침에 보내준 짧은 메시지가 생각났어."
        },
        {
          "displayName": "도윤",
          "content": "퇴근길에 같이 걷던 장면."
        }
      ]
    }
  ]
}
```

### POST /api/exports/{exportRequestId}/complete
Response:
```json
{
  "exportRequestId": 300,
  "status": "COMPLETED",
  "completedAt": "2026-04-22T10:00:00",
  "downloads": [
    {
      "format": "json",
      "url": "/api/exports/300/download?format=json"
    },
    {
      "format": "text",
      "url": "/api/exports/300/download?format=text"
    }
  ]
}
```

### POST /api/exports/{exportRequestId}/cancel
Response:
```json
{
  "exportRequestId": 300,
  "status": "CANCELLED"
}
```

### DELETE /api/exports/{exportRequestId}
Response:
```json
{
  "exportRequestId": 300,
  "deleted": true
}
```

### GET /api/exports/{exportRequestId}/download?format=json
Response headers:
```text
Content-Type: application/json; charset=UTF-8
Content-Disposition: attachment; filename="couple-diary-300.json"
```

Response:
```json
{
  "exportRequestId": 300,
  "coupleId": 10,
  "exportedAt": "2026-04-22T10:00:00",
  "entries": [
    {
      "date": "2026-04-22",
      "question": "오늘 가장 먼저 떠오른 서로의 모습은 무엇인가요?",
      "answers": [
        {
          "displayName": "민지",
          "content": "아침에 보내준 짧은 메시지가 생각났어."
        },
        {
          "displayName": "도윤",
          "content": "퇴근길에 같이 걷던 장면."
        }
      ]
    }
  ]
}
```

### GET /api/exports/{exportRequestId}/download?format=text
Response headers:
```text
Content-Type: text/plain; charset=UTF-8
Content-Disposition: attachment; filename="couple-diary-300.txt"
```

Response body:
```text
2026-04-22
Q. 오늘 가장 먼저 떠오른 서로의 모습은 무엇인가요?

민지
아침에 보내준 짧은 메시지가 생각났어.

도윤
퇴근길에 같이 걷던 장면.
```

## 상태 관리
- 서버 상태는 Spring Boot API 응답을 기준으로 한다.
- Frontend 전역 상태는 최소화하고 인증 확인은 GET /api/auth/me 응답을 사용한다.
- 답변 작성 폼, export 체크 목록, 주문 내역 탭, 주문 미리보기 단계 같은 화면 내부 상태는 useState 또는 useReducer로 관리한다.
- 답변 작성 폼은 자동 저장하지 않는다. 저장 전 dirty 상태에서 이탈하려 하면 확인 UI를 표시한다.
- API 요청 중 동일 액션 버튼은 disabled 처리한다.
- 캐싱이 필요해지면 TanStack Query 도입을 검토하되 MVP 기본 범위에는 포함하지 않는다.

## 보안 및 개인정보
- 비밀번호는 BCrypt로 해시한다.
- 세션에는 userId만 저장한다.
- CORS 허용 origin은 환경 변수로 바꿀 수 있으며 로컬 기본값은 http://localhost:3000이다. credentials 요청을 허용한다.
- 답변 본문, 비밀번호, 세션 ID를 애플리케이션 로그에 남기지 않는다.
- 커플 소유권 검증은 controller가 아니라 service에서 공통으로 수행한다.
- 주문 완료 시 저장되는 json_payload와 text_payload는 답변 본문을 포함하므로 답변과 같은 수준의 개인정보로 취급한다.

## 테스트 전략
- Service 단위 테스트: 회원가입, 로그인, 초대 코드, 오늘 질문 배정, 답변 공개 규칙, 주문 상태 전이
- Repository 테스트: unique 제약, 날짜별 질문 조회, 커플별 기록 조회
- Controller 테스트: 세션 인증, 권한 거부, validation error, 다운로드 상태 제한, 전역 예외 처리 응답 형식
- Error handling 테스트: ErrorCode별 HTTP status, validation fields, 500 fallback, 민감정보 비노출
- Frontend 테스트: 라우팅 가드, 로그인 폼, 답변 작성 폼, 잠금 상태 렌더링, 주문 3단계 화면 전환, 빈 상태/비활성 상태, 모바일 터치 영역

## 로컬 실행 기준
- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- Database: Docker Compose MySQL (호스트 `3307` -> 컨테이너 `3306`)
- Frontend 환경 변수: `frontend/.env.local`의 `NEXT_PUBLIC_API_BASE_URL` 기본값은 `http://localhost:8080`
- Backend 환경 변수: `backend/.env`를 optional import하며 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `CORS_ALLOWED_ORIGIN`을 덮어쓸 수 있다.
- Backend 실행 명령: `./gradlew run` 또는 Windows PowerShell에서 `.\gradlew.bat run`
- CORS 허용 origin 기본값은 `http://localhost:3000`이며 `CORS_ALLOWED_ORIGIN`으로 변경할 수 있다.
