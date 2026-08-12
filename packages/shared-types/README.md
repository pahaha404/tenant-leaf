# shared-types

Android 앱과 Kotlin 서버가 함께 참조하는 데이터 계약을 정의합니다.

계약의 기준 문서는 [`docs/technical/api-spec.md`](../../docs/technical/api-spec.md)입니다. 이 패키지의 타입과 상태값은 확정된 계약 1.0을 기준으로 구현합니다.

## 예정 타입

- 매물
- 임장
- 체크리스트 항목과 상태
- 촬영 프레임
- AI 탐지 결과
- AI 분석 상태

## 목적

앱과 서버의 상태 이름·필드 이름이 다르게 구현되는 문제를 줄입니다. 실제 Kotlin 코드는 Gradle 공통 모듈 또는 OpenAPI 코드 생성 방식으로 공유합니다.
