# 백엔드 Task

- [x] JDK 21·Docker Desktop 설치와 버전 확인
- [ ] API 빌드·테스트 기본 환경 구성
- [x] PostgreSQL 로컬 개발 환경 구성
- [x] MVP 데모 계정 흐름 결정
- [x] 확정 API의 OpenAPI 명세 작성과 문법 검증
- [x] OpenAPI 기반 Spring Boot Kotlin 타입·API 인터페이스 자동 생성
- [x] 매물 CRUD API 구현
- [x] 전체 서비스 흐름 기반 도메인 규칙 초안 작성
- [x] 실시간 스트리밍 분석 폐지와 기본 영상 촬영·JPEG 분석 계약 1.2 반영
- [x] 구역·관찰·근거 미디어 중심 공통 계약 2.0 검토 초안과 OpenAPI 확정 범위 반영·생성 검증
- [x] JPEG 미디어 배치 등록·완료·재시도·조회 계약 2.1과 OpenAPI 생성 검증
- [x] 임장 시작·목록·상세·종료·취소 API 구현
- [ ] 임장 보관 정책 확정과 API 구현
- [ ] 업로드 URL 발급과 미디어 메타데이터 API 구현
- [ ] 구역별 분석 상태·관찰 결과 API 구현
- [ ] AI 작업 요청·상태·결과 조회 API 구현
- [ ] 보고서 생성·조회 API 구현
- [ ] API 오류 형식과 권한 오류 테스트
- [ ] Android 연동 확인

## 완료 기록

## 진행 기록

- 2026-08-24 — 리포트 생성 중 전체 분석 대상이 `0 / 0장`으로 표시되던 계약 오류를 수정함. `ReportSummary.totalMediaCount`를 확정 미디어 수에서 제공하고 성공·실패 수와 분리했으며, 원격 Gradle 플러그인 접근 제한으로 서버 전체 테스트는 미검증 상태임.
- 2026-08-24 — 분석 완료 JPEG의 원시 탐지를 사용자 관찰로 변환하고 자동 리포트를 집계하는 DB V9·Spring Boot API·주기적 조정 작업을 구현함. 관찰별 근거와 동일 사진의 다중 픽셀 `xyxy` bbox, 짧은 만료 조회 URL, 잠정 참고 점수를 응답하도록 연결했으나 Gradle 플러그인 원격 해석 제한으로 서버 컴파일·통합 테스트는 미검증이므로 관련 체크 항목은 `[ ]`로 유지함.
- 2026-08-24 — 매물 삭제 정책을 소프트 삭제(Soft Delete, `deleted_at`)로 전환함. Flyway 마이그레이션 `V8__add_deleted_at_to_properties.sql`을 추가하고, `PropertyEntity`, `PropertyRepository`, `PropertyService`, `InspectionService`에 `deletedAtIsNull` 조회를 적용함. 임장 기록이 있는 매물도 외래키 제약 충돌 없이 안전하게 삭제(보관/숨김 처리)되어 목록과 상세 조회에서 제외되며, 과거 임장 세션 및 리포트 데이터의 무결성은 영구 보존됨. `server/api` 22개 전체 단위/통합 테스트 통과 (`BUILD SUCCESSFUL`).

- 2026-08-20 — 로컬 기본 설정과 실배포 `prod` 프로필을 분리했다. 실배포 DB·객체 저장소 설정은 배포 환경변수가 없으면 해석되지 않도록 구성했고, `clean test`로 검증했다. 실제 배포 인프라와 운영 DB 연결은 배포처 확정 전이므로 미검증이다.

- 2026-08-19 — MinIO Compose 구성, `media`·멱등성 레코드 Flyway 테이블, 업로드 등록·완료·재시도·목록·상세 Spring Boot 구현과 저장소 대체 테스트를 추가함. OpenAPI 검사와 Kotlin 주·테스트 소스 컴파일은 통과했으나 현재 실행 환경에서 MinIO SDK 다운로드와 Docker 실행이 불가능하여 실제 MinIO PUT 통합 검증 전까지 작업 체크는 미완료로 유지함.

- 2026-08-19 — OpenAPI 2.0 확정 범위에 맞춰 PostgreSQL 임장 테이블과 Kotlin Entity·Repository·Service·Controller를 구현함. `IN_PROGRESS → ENDED/CANCELLED` 단방향 전이, 소유권 은닉 조회, 임장 있는 매물 삭제 보호와 오류 응답을 별도 `tenant_leaf_test` 스키마의 통합 테스트 및 `clean openApiValidate test`로 확인함. 미확정 미디어·관찰·리포트와 임장 보관 API는 추가하지 않음.
- 2026-08-19 — JPEG 미디어 업로드 계약 2.1에서 요청당 1~20장 등록, 15분 서명 URL, 완료·재시도·목록·상세 API와 `Idempotency-Key` 충돌 규칙을 확정함. OpenAPI 검사·Kotlin 생성·계약 테스트를 통과했으며 객체 저장소와 서버 API 구현은 후속 작업으로 유지함.
