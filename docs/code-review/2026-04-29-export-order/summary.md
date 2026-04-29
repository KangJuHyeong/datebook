# Code Review Summary

검토 범위: 주문 내역/상세/삭제 기능 추가와 관련된 backend/frontend 변경사항.

가장 먼저 고칠 항목은 백엔드 테스트 재현성 문제입니다. 현재 H2 드라이버가 명시 의존성으로 선언되어 있지 않아 컨트롤러 통합 테스트가 실행되지 않습니다. 그 다음으로는 주문 ID 존재 여부 노출을 줄이기 위한 소유권 조회 방식 변경, 프론트의 빈 기록 상태에서도 주문 내역 접근을 허용하는 UI 흐름 수정이 중요합니다.

리뷰 파일:

- `docs/code-review/2026-04-29-export-order/backend.md`
- `docs/code-review/2026-04-29-export-order/frontend.md`
