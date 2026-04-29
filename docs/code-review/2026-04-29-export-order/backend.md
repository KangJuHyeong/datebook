# Backend Code Review

검토 범위: `backend/src/main/java`, `backend/src/test/java`, export/order 흐름을 포함한 인증/답변/일기/커플 API.

검증:
- `cd backend && .\gradlew.bat test`: 통과
- `cd backend && .\gradlew.bat build`: 통과

## 조치 완료

- CSRF 보호를 활성화하고 `GET /api/auth/csrf` 토큰 발급 API를 추가했습니다. CSRF 실패를 포함한 Spring Security 403 응답도 공통 JSON 오류 형식으로 반환합니다.
- 로그인/회원가입 성공 시 `HttpServletRequest.changeSessionId()`로 세션 ID를 재발급하도록 변경했습니다.
- controller 통합 테스트의 상태 변경 요청에 CSRF 토큰을 명시하고, CSRF 토큰 조회/누락 시나리오를 추가했습니다.

## 발견 사항

- 문제점: 세션 쿠키 기반 인증을 사용하면서 CSRF 보호가 전역 비활성화되어 있습니다. `SecurityConfig`에서 `csrf.disable()`을 설정하고 모든 요청을 `permitAll()`로 열어 둔 뒤 서비스 계층에서 세션을 검사하는 구조라, 사용자가 로그인된 브라우저로 악성 페이지를 방문하면 답변 수정, 주문 생성/완료/삭제 같은 상태 변경 요청이 쿠키와 함께 전송될 수 있습니다. 관련 위치: `backend/src/main/java/app/config/SecurityConfig.java:25`.
- 심각도: 높음
- 권장 해결책: Spring Security CSRF를 활성화하고, Next.js BFF가 CSRF 토큰을 쿠키 또는 헤더로 전달하도록 구성하세요. 최소한 `POST`, `PUT`, `DELETE` 보호 API에는 `SameSite=Lax/Strict` 세션 쿠키와 별도 CSRF 토큰 검증을 함께 적용하는 것이 안전합니다.

- 문제점: 로그인/회원가입 성공 시 기존 세션에 바로 `userId`를 저장합니다. 세션 ID를 재발급하지 않으면 세션 고정 공격에 취약해질 수 있습니다. 관련 위치: `backend/src/main/java/app/service/AuthService.java:50`, `backend/src/main/java/app/service/AuthService.java:61`.
- 심각도: 높음
- 권장 해결책: 인증 성공 직후 `request.changeSessionId()`를 호출하거나 기존 세션을 무효화한 뒤 새 세션을 발급하세요. 현재 서비스는 `HttpSession`만 받으므로 `HttpServletRequest`를 받도록 컨트롤러/서비스 계약을 바꾸거나 인증 전용 헬퍼를 두는 방식을 권장합니다.

- 문제점: 로그인 실패에 대한 rate limit, 계정 잠금, 지연 응답 정책이 없습니다. 비밀번호 해시는 BCrypt라 기본은 갖췄지만, `/api/auth/login`에 반복 요청을 보내는 credential stuffing을 서버가 제어하지 못합니다. 관련 위치: `backend/src/main/java/app/service/AuthService.java:57`.
- 심각도: 중간
- 권장 해결책: IP와 이메일 기준 실패 카운터를 두고 짧은 시간 안의 반복 실패를 제한하세요. 로컬 MVP라도 서비스 계층 테스트로 “N회 실패 후 제한” 정책을 고정해두면 이후 운영 환경으로 옮길 때 안전합니다.

- 문제점: `application.yml`이 기본 실행에서 `ddl-auto: update`와 `sql.init.mode: always`를 사용합니다. 앱 시작 때마다 스키마를 자동 변경하고 seed SQL을 재실행할 수 있어, 로컬 데이터가 의도치 않게 바뀌거나 운영 설정으로 복사될 때 위험합니다. 관련 위치: `backend/src/main/resources/application.yml:13`, `backend/src/main/resources/application.yml:22`.
- 심각도: 중간
- 권장 해결책: 로컬 전용 profile에만 `update`와 seed 실행을 두고, 기본 설정은 `validate` 또는 migration 기반으로 분리하세요. MVP 범위에서도 `application-local.yml`, `application-test.yml`처럼 profile별 책임을 명확히 나누는 편이 좋습니다.

- 문제점: 일기 목록 API가 커플의 전체 `DailyQuestion`과 전체 답변을 한 번에 로드합니다. 데이터가 쌓이면 `/api/diary` 응답 크기와 메모리 사용량이 계속 증가합니다. 관련 위치: `backend/src/main/java/app/service/DiaryService.java:50`, `backend/src/main/java/app/service/DiaryService.java:55`.
- 심각도: 중간
- 권장 해결책: 페이지네이션 또는 월 단위 조회를 추가하세요. Repository에는 `Pageable` 기반 메서드를 두고, 답변은 해당 페이지의 dailyQuestionId에 대해서만 batch 조회하면 됩니다.

- 문제점: 주문 생성 검증에서 `isFullyAnswered`가 질문마다 커플 멤버를 다시 조회합니다. 선택한 기록 수만큼 같은 쿼리가 반복될 수 있습니다. 관련 위치: `backend/src/main/java/app/service/ExportService.java:247`, `backend/src/main/java/app/service/ExportService.java:262`.
- 심각도: 중간
- 권장 해결책: `loadValidExportDailyQuestions` 시작 시 커플 멤버 목록을 한 번만 조회해 `Set<Long> memberIds`로 넘기세요. 이렇게 하면 선택 항목 수가 늘어도 커플 멤버 조회는 1회로 고정됩니다.

- 문제점: 완료/미리보기 주문 상세에서 snapshot JSON 파싱 실패를 모두 `INTERNAL_ERROR`로만 변환하고 원인 로그를 남기지 않습니다. 저장된 payload 손상, 코드 호환성 문제, 서버 오류가 운영 중 구분되지 않습니다. 관련 위치: `backend/src/main/java/app/service/ExportService.java:312`.
- 심각도: 낮음
- 권장 해결책: `exportRequestId`와 예외 타입을 포함한 서버 로그를 남기고, 필요하면 `EXPORT_SNAPSHOT_INVALID` 같은 내부 오류 코드를 분리하세요. 사용자 응답은 일반 오류여도 괜찮지만 운영자는 원인을 추적할 수 있어야 합니다.

- 문제점: 인증 사용자 조회 로직이 여러 서비스에 중복되어 있습니다. `AuthService`, `AnswerService`, `DiaryService`, `ExportService`, `DailyQuestionService`, `CoupleService`가 세션에서 `userId`를 꺼내고 사용자/커플 멤버를 확인하는 패턴을 반복합니다. 관련 위치 예: `backend/src/main/java/app/service/ExportService.java:407`.
- 심각도: 낮음
- 권장 해결책: `CurrentUserProvider` 또는 `SessionUserResolver` 같은 공통 컴포넌트로 추출하세요. 보호 API의 소유권 검증이 핵심 규칙이므로 중복을 줄이면 누락 위험도 같이 줄어듭니다.
