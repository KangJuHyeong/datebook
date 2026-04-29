# Frontend Code Review

검토 범위: `frontend/src/app`, `frontend/src/lib`, `frontend/src/features`, export/order UI와 BFF route handler.

검증:
- `cd frontend && npm run test`: 통과
- `cd frontend && npm run build`: 최초 sandbox 실행은 사용자 경로 `lstat` 권한 문제로 실패, 승인 후 재실행 통과

## 조치 완료

- BFF 공통 프록시의 요청/응답 헤더 전달을 allowlist 방식으로 제한했습니다.
- 프론트 API client가 `POST`, `PUT`, `PATCH`, `DELETE` 요청 전에 CSRF 토큰을 확보하고 `X-XSRF-TOKEN` 헤더로 전달하도록 변경했습니다.
- JSON이 아닌 오류 응답도 `ApiError`로 정규화하도록 파싱을 방어적으로 변경하고 단위 테스트를 추가했습니다.

## 발견 사항

- 문제점: BFF 공통 프록시가 브라우저 요청 헤더를 거의 그대로 Spring API로 전달합니다. 현재는 `host`, `content-length`만 제거하므로 hop-by-hop 헤더, 원치 않는 forwarding 헤더, 클라이언트가 조작한 헤더가 백엔드까지 들어갈 수 있습니다. 관련 위치: `frontend/src/lib/server/backend.ts:11`.
- 심각도: 중간
- 권장 해결책: 전달 헤더를 allowlist 방식으로 바꾸세요. 예를 들어 `cookie`, `content-type`, `accept`, `x-csrf-token`처럼 필요한 헤더만 복사하고, 응답 헤더도 `set-cookie`, `content-type`, `content-disposition` 등 필요한 값 중심으로 명시적으로 전달하는 편이 안전합니다.

- 문제점: API JSON 파서가 `JSON.parse` 실패를 처리하지 않습니다. 백엔드나 프록시가 HTML 오류 페이지, 빈 오류 본문, 깨진 JSON을 반환하면 `ApiError`가 아니라 `SyntaxError`가 UI까지 올라와 공통 오류 처리와 redirect/refetch 정책이 깨질 수 있습니다. 관련 위치: `frontend/src/lib/api/runtime.mjs:80`.
- 심각도: 중간
- 권장 해결책: `JSON.parse`를 `try/catch`로 감싸고, 실패 시 `parseApiErrorPayload(null, response.status)`를 사용해 항상 `ApiError` 형태로 정규화하세요. 성공 응답도 JSON이 아니면 명시적인 클라이언트 오류로 처리하는 테스트를 추가하면 좋습니다.

- 문제점: `ExportClient`가 주문 선택, 미리보기, 완료, 완료 상세, 주문 목록, 삭제, 다운로드 상태를 모두 직접 관리합니다. 상태가 15개 이상으로 늘어나면서 `flowStep`, `activeTab`, `preview`, `completedOrder`, `completedDetail`, `exportRequestId` 조합의 유효성을 사람이 계속 맞춰야 합니다. 관련 위치: `frontend/src/app/export/export-client.tsx:126`.
- 심각도: 중간
- 권장 해결책: 화면 컴포넌트는 `app/export/` 안에 유지하되, 상태 전이는 `useExportOrderFlow` 같은 route-local hook 또는 reducer로 묶으세요. 가능한 상태를 discriminated union으로 모델링하면 “완료 화면인데 preview가 null” 같은 불가능한 상태를 타입으로 막을 수 있습니다.

- 문제점: 날짜/시간 포맷팅이 offset 없는 `LocalDateTime` 문자열을 브라우저 로컬 시간대로 파싱합니다. 백엔드는 UTC 기준 `LocalDateTime`을 내려주므로 사용자 환경에 따라 완료/수정 시간이 다르게 보일 수 있습니다. 관련 위치: `frontend/src/app/export/export-client.tsx:78`, `frontend/src/app/today/today-client.tsx:15`.
- 심각도: 중간
- 권장 해결책: 백엔드 응답을 `OffsetDateTime` 또는 `Instant` 기반 ISO-8601 문자열로 바꾸고, 프론트는 명시적인 timezone 정책으로 표시하세요. 구조 변경 전에는 응답 시간이 UTC 문자열임을 전제로 `Z`를 붙여 파싱하는 임시 헬퍼를 공통화할 수 있습니다.

- 문제점: 완료 주문 삭제 확인에 `window.confirm`을 직접 사용합니다. 브라우저 기본 확인창은 스타일/접근성/테스트 제어가 제한되고, 앱의 로딩 상태나 위험 안내 문구와 일관되게 다루기 어렵습니다. 관련 위치: `frontend/src/app/export/export-client.tsx:374`.
- 심각도: 낮음
- 권장 해결책: route-local 확인 모달을 추가해 `aria-modal`, focus 이동, 취소/삭제 버튼, pending 상태를 명확히 제공하세요. Playwright에서도 취소와 확인 경로를 안정적으로 검증할 수 있습니다.

- 문제점: 다운로드 파일명 파싱이 `filename="..."` 단순 정규식에만 의존합니다. RFC 5987의 `filename*=` 또는 세미콜론/이스케이프가 포함된 파일명은 제대로 처리하지 못할 수 있습니다. 관련 위치: `frontend/src/app/export/export-client.tsx:72`.
- 심각도: 낮음
- 권장 해결책: `Content-Disposition` 파싱 유틸을 별도로 두고 `filename*=`를 우선 처리하세요. 현재 백엔드 파일명은 단순 ASCII라 즉시 장애 가능성은 낮지만, 다운로드 기능은 작은 유틸 테스트로 고정해두기 좋습니다.

- 문제점: export 탭 UI가 실제 탭 패턴 대신 `aria-pressed` 버튼 두 개로 구현되어 있습니다. 기능상 동작은 가능하지만 보조기술 사용자에게 “탭 목록/탭 패널” 관계가 명확하지 않습니다. 관련 위치: `frontend/src/app/export/export-tabs.tsx:9`.
- 심각도: 낮음
- 권장 해결책: 탭 의도가 강한 UI라면 `role="tablist"`, `role="tab"`, `aria-selected`, `aria-controls`를 사용하고 패널에 연결하세요. 단순 토글 버튼으로 유지할 경우에는 label을 “주문 화면 보기 방식 선택”처럼 동작에 맞게 조정하는 것이 좋습니다.
