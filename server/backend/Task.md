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
- [x] 임장 시작·목록·상세·종료·취소 API 구현
- [ ] 임장 보관 정책 확정과 API 구현
- [ ] 업로드 URL 발급과 미디어 메타데이터 API 구현
- [ ] 구역별 분석 상태·관찰 결과 API 구현
- [ ] AI 작업 요청·상태·결과 조회 API 구현
- [ ] 보고서 생성·조회 API 구현
- [ ] API 오류 형식과 권한 오류 테스트
- [ ] Android 연동 확인

## 완료 기록

- 2026-08-19 — OpenAPI 2.0 확정 범위에 맞춰 PostgreSQL 임장 테이블과 Kotlin Entity·Repository·Service·Controller를 구현함. `IN_PROGRESS → ENDED/CANCELLED` 단방향 전이, 소유권 은닉 조회, 임장 있는 매물 삭제 보호와 오류 응답을 별도 `tenant_leaf_test` 스키마의 통합 테스트 및 `clean openApiValidate test`로 확인함. 미확정 미디어·관찰·리포트와 임장 보관 API는 추가하지 않음.
