# Frontend Code Review

## 검증

- `cd frontend && npm run test`: 통과
- 소스 기반 테스트는 통과했지만, 주문 내역 UI의 실제 브라우저 상호작용과 빈 기록 상태에서의 주문 내역 접근은 E2E로 검증되지 않았습니다.

## 발견사항

- 문제점: `ExportClient`가 `entries.length === 0`이면 즉시 `DiaryEmptyState`를 반환하므로 주문 내역 탭에 접근할 수 없습니다. 완료 주문이나 미리보기 주문이 이미 존재해도 현재 다이어리 목록이 비어 있으면 주문 재개/다운로드/삭제 플로우가 막힙니다. 관련 위치: `frontend/src/app/export/export-client.tsx`의 early return.
- 심각도: 중간
- 권장 해결책: 빈 기록 상태에서도 탭 UI는 렌더링하고, `activeTab === "select"`일 때만 기록 empty state를 보여주세요. 주문 내역은 `entries`와 독립적으로 조회/표시되도록 분리하는 것이 좋습니다.

- 문제점: 주문 내역 추가로 `export-client.tsx`가 선택, 미리보기, 완료, 완료 상세, 목록 조회, 삭제, 다운로드까지 모두 포함하는 큰 Client Component가 됐습니다. 상태 전이가 많아지면서 `submitting`, `ordersLoading`, `deletingOrderId`, `flowStep`, `activeTab`의 조합을 추론하기 어렵습니다.
- 심각도: 중간
- 권장 해결책: route-local 규칙을 유지하면서 같은 폴더 안에 `order-list`, `order-detail`, `selection-panel` 같은 작은 컴포넌트로 분리하거나, 상태 전이만 `useExportOrderFlow` 훅으로 추출하세요. 화면 panel/container를 `features/`에 만들지는 말고 `app/export/` 안에서 분리하는 편이 현재 아키텍처와 맞습니다.

- 문제점: 주문 내역 API 클라이언트와 route handler를 확인하는 테스트가 정규식 기반 소스 검사에 머물러 있습니다. `GET /api/exports`, `GET /api/exports/:id`, `DELETE /api/exports/:id`가 실제 fetch 옵션, credentials, 오류 상태를 제대로 처리하는지는 검증하지 않습니다.
- 심각도: 낮음
- 권장 해결책: `getExportOrders`, `getExportOrderDetail`, `deleteExportOrder`에 대해 `apiRequestJson` mock을 사용하는 단위 테스트를 추가하고, Playwright에는 주문 내역 탭에서 미리보기 주문 재개와 완료 주문 상세 조회를 검증하는 경로를 추가하세요.

- 문제점: `formatDateTime`이 서버에서 내려온 timezone 없는 `LocalDateTime` 문자열을 브라우저 로컬 타임존 기준으로 파싱합니다. 백엔드가 UTC로 저장한 값을 timezone 없이 직렬화하면 한국 사용자의 화면에서 완료/미리보기 시각이 의도와 다르게 보일 수 있습니다.
- 심각도: 낮음
- 권장 해결책: 백엔드는 `OffsetDateTime` 또는 ISO-8601 offset 포함 문자열을 내려주고, 프론트는 명시적인 timezone 기준으로 포맷하세요. MVP에서 당장 구조 변경이 부담되면 API 타입 주석과 표시 정책을 문서화하고 테스트에 대표 날짜를 고정하세요.

- 문제점: 삭제 확인에 `window.confirm`을 직접 사용하고 있어 접근성/일관성/테스트성이 떨어집니다. 현재 UI는 상태를 배지와 문구로 잘 보강하고 있는데, 삭제 같은 파괴적 액션만 브라우저 기본 대화상자에 의존합니다.
- 심각도: 낮음
- 권장 해결책: route-local 확인 모달 또는 재사용 가능한 confirm 컴포넌트를 사용해 포커스 이동, 취소/확인 버튼, `aria-modal`, 로딩 상태를 명시하세요. Playwright에서도 삭제 취소/확인 흐름을 안정적으로 테스트할 수 있습니다.
