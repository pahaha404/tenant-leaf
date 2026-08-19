# shared-types

Android 앱과 Kotlin 서버가 함께 참조하는 데이터 계약을 정의합니다.

계약의 기준 문서는 [`공통 api 계약.md`](../../team/00_shared/공통%20api%20계약.md)입니다. 이 패키지의 타입과 상태값은 체크리스트 중심 계약을 교체하는 검토 초안 2.0을 기준으로 생성합니다.

기계가 읽고 검증할 수 있는 명세는 [`openapi/openapi.yaml`](openapi/openapi.yaml)에 있습니다. 아직 상세 형식이 확정되지 않은 미디어 업로드, 관찰, 리포트, 메모와 AI 실패 결과 API는 명세에서 제외합니다.

## 현재 생성 범위

- 데모 사용자와 인증 응답
- 매물
- 임장 생명주기와 서버 집계 분석 상태
- 구역, 미디어, 관찰과 리포트의 확정 상태 enum
- AI 원본 라벨 13개와 정규화 바운딩 박스

체크리스트·기존 Frame·Detection HTTP 계약은 최신 도메인 규칙과 충돌해 제거했습니다. 새 Media·Observation·Report 요청·응답은 남은 P0 항목이 합의된 뒤 추가합니다.

## 목적

앱과 서버의 상태 이름·필드 이름이 다르게 구현되는 문제를 줄입니다. 실제 Kotlin 코드는 Gradle 공통 모듈 또는 OpenAPI 코드 생성 방식으로 공유합니다.

## Spring Boot 서버 코드 생성

`server/api`에서 다음 명령을 실행하면 명세 문법을 검사하고 Kotlin 타입과 API 인터페이스를 생성합니다.

```powershell
.\gradlew.bat openApiValidate openApiGenerate
```

생성 위치는 `server/api/build/generated/openapi`이며 Git에 저장하지 않습니다. 생성 코드를 직접 수정하지 말고 이 폴더의 `openapi/openapi.yaml`을 수정합니다.
