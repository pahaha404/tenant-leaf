# shared-types

Android 앱과 Kotlin 서버가 함께 참조하는 데이터 계약을 정의합니다.

계약의 기준 문서는 [`../technical/api-spec.md`](../technical/api-spec.md)입니다. 이 패키지의 타입과 상태값은 확정된 계약 1.1을 기준으로 구현합니다.

기계가 읽고 검증할 수 있는 명세는 [`openapi/openapi.yaml`](openapi/openapi.yaml)에 있습니다. 아직 상세 형식이 확정되지 않은 리포트, 매물 비교, 위험도와 AI 실패 결과는 명세에서 제외합니다.

## 예정 타입

- 매물
- 임장
- 체크리스트 항목과 상태
- 촬영 프레임
- AI 탐지 결과
- AI 분석 상태

## 목적

앱과 서버의 상태 이름·필드 이름이 다르게 구현되는 문제를 줄입니다. 실제 Kotlin 코드는 Gradle 공통 모듈 또는 OpenAPI 코드 생성 방식으로 공유합니다.

## Spring Boot 서버 코드 생성

`server/api`에서 다음 명령을 실행하면 명세 문법을 검사하고 Kotlin 타입과 API 인터페이스를 생성합니다.

```powershell
.\gradlew.bat openApiValidate openApiGenerate
```

생성 위치는 `server/api/build/generated/openapi`이며 Git에 저장하지 않습니다. 생성 코드를 직접 수정하지 말고 이 폴더의 `openapi/openapi.yaml`을 수정합니다.
