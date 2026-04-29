# Code Review Summary

리뷰 일자: 2026-04-29

리뷰 범위:
- Backend: Spring Boot API, 세션 인증, 답변/일기/커플/주문 서비스와 테스트
- Frontend: Next.js App Router, BFF route handler, API client, export/order UI

검증 결과:
- `cd backend && .\gradlew.bat test`: 통과
- `cd backend && .\gradlew.bat build`: 통과
- `cd frontend && npm run test`: 통과
- `cd frontend && npm run build`: sandbox 권한 문제로 1회 실패 후 승인 재실행 통과

가장 먼저 고칠 항목:
1. 세션 쿠키 인증에서 CSRF 보호가 꺼져 있는 문제
2. 로그인/회원가입 후 세션 ID를 재발급하지 않는 문제
3. Next.js BFF가 요청 헤더를 allowlist 없이 백엔드로 전달하는 문제
4. offset 없는 날짜/시간 문자열을 브라우저 로컬 시간대로 파싱하는 문제
5. 일기/주문 생성 검증에서 데이터가 늘어날 때 커지는 조회 비용

리뷰 문서:
- `docs/code-review/2026-04-29-export-order/backend.md`
- `docs/code-review/2026-04-29-export-order/frontend.md`

## 2026-04-29 조치 내역

- CSRF 보호를 활성화하고 `/api/auth/csrf` 토큰 발급 흐름을 추가했습니다.
- 로그인/회원가입 성공 시 세션 ID를 재발급하도록 변경했습니다.
- Next.js BFF 프록시의 요청/응답 헤더 전달을 allowlist 방식으로 제한했습니다.
- 프론트 API client가 unsafe method 요청 전에 CSRF 토큰을 확보해 `X-XSRF-TOKEN` 헤더로 전달하도록 변경했습니다.
- 깨진 JSON 오류 응답도 `ApiError` 형태로 정규화되도록 파싱을 방어적으로 바꿨습니다.
