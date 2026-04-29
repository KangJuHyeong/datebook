# Backend Code Review

## 검증

- `cd frontend && npm run test`: 통과
- `cd backend && .\gradlew.bat test --tests app.service.ExportServiceTest --tests app.controller.ExportControllerIntegrationTest`: 실패
  - `ExportServiceTest`: 통과
  - `ExportControllerIntegrationTest`: ApplicationContext 로딩 실패
  - 원인: `application-test.yml`은 `org.h2.Driver`를 요구하지만 `backend/build.gradle`은 H2를 명시 의존성으로 선언하지 않음

## 발견사항

- 문제점: 백엔드 통합 테스트가 로컬 Gradle 캐시에 있는 jar 묶음에 의존하고 있어 H2 드라이버가 캐시에 없으면 전체 컨트롤러 테스트가 실행되지 않습니다. `backend/src/test/resources/application-test.yml`은 `org.h2.Driver`를 사용하지만, `backend/build.gradle`에는 `testImplementation 'com.h2database:h2'`가 명시되어 있지 않습니다.
- 심각도: 높음
- 권장 해결책: `backend/build.gradle.docker`처럼 `backend/build.gradle`에도 Spring Boot/JPA/Test/H2/MySQL 의존성을 명시적으로 선언하거나, 최소한 `testImplementation 'com.h2database:h2'`를 추가하세요. 현재처럼 `gradleCacheJars` 전체를 implementation/testImplementation에 넣는 방식은 재현 가능한 테스트 환경을 보장하지 못합니다.

- 문제점: 다른 커플의 주문 ID로 상세/삭제/다운로드 등을 호출하면 `getOwnedExportRequest`가 먼저 `findById`로 존재 여부를 확인한 뒤 커플이 다르면 `FORBIDDEN`을 반환합니다. 이 동작은 공격자가 주문 ID 존재 여부를 403/404 차이로 추측할 수 있게 합니다. 관련 위치: `backend/src/main/java/app/service/ExportService.java`의 `getOwnedExportRequest`.
- 심각도: 중간
- 권장 해결책: 이미 선언된 `findByIdAndCouple_Id(exportRequestId, coupleId)`를 사용해 소유 커플의 주문만 조회하고, 없으면 동일하게 `NOT_FOUND`를 반환하세요. 권한 오류가 꼭 필요하다면 내부 로깅만 남기고 외부 응답은 404로 통일하는 편이 안전합니다.

- 문제점: 주문 목록 조회에서 주문마다 `exportItemRepository.countByExportRequest_Id`를 호출해 N+1 쿼리가 발생합니다. 주문 내역이 많아지면 `/api/exports` 응답 시간이 선형으로 증가합니다. 관련 위치: `ExportService.listExports`와 `toOrderSummary`.
- 심각도: 중간
- 권장 해결책: `ExportRequest`와 `ExportItem`을 group by로 묶는 projection 쿼리 또는 DTO 전용 repository 메서드를 추가해 목록과 item count를 한 번에 가져오세요. MVP라면 페이지 크기 제한을 먼저 추가하고, count batch 조회를 뒤따르게 해도 됩니다.

- 문제점: 완료 처리 시 `completeExport`가 미리보기에서 사용자에게 보여준 entries가 아니라 완료 시점의 원본 답변을 다시 조회해 스냅샷을 생성합니다. 미리보기 후 완료 전 답변이 수정되면 사용자가 확인하지 않은 내용이 완료 스냅샷에 저장될 수 있습니다.
- 심각도: 중간
- 권장 해결책: 미리보기 시점의 JSON/text payload 또는 preview snapshot을 `ExportRequest`에 저장한 뒤 완료 시 그 스냅샷을 확정하세요. 또는 완료 직전에 diff/재확인 단계를 강제하고 테스트로 “미리보기 후 답변 변경” 시나리오를 추가하세요.

- 문제점: 완료 주문 상세에서 JSON 스냅샷 파싱 실패를 모두 `INTERNAL_ERROR`로 변환합니다. 저장된 주문 데이터 손상과 서버 장애가 구분되지 않아 운영 중 원인 파악이 어렵습니다.
- 심각도: 낮음
- 권장 해결책: 스냅샷 파싱 실패 전용 에러 코드와 구조화 로그를 추가하세요. 사용자 응답은 일반 오류여도 괜찮지만, 서버 로그에는 `exportRequestId`와 파싱 실패 원인이 남아야 합니다.
