# AI 글래스 연동 작업 (AIGlassTask)

스마트 글래스(Meta Ray-Ban 및 Mock)를 안드로이드 앱에 연동하고, UI와 백엔드가 활용할 수 있도록 클린 아키텍처 기반의 Glass API를 구축하는 작업 목록입니다.

---

## 📋 단계별 작업 목록

### [x] 1단계: 연결 상태 인터페이스 & 도메인 모델 정의
- [x] 독립적인 상태 머신 직교화 분리 (`GlassRegistrationStatus`, `GlassLinkStatus`, `GlassStreamStatus`, `GlassAudioRouteStatus`)
- [x] Null-Safe 배터리 및 검증 로직이 포함된 기기 정보 모델 (`GlassDeviceInfo`)
- [x] UI 복구 액션(`ErrorRecoveryAction`) 메타데이터가 포함된 도메인 에러 모델 (`GlassError`)
- [x] UI 상태 바인딩용 통합 불변 상태 모델 (`GlassState`: `isReadyForInspection`, `isBusy`, `isStreaming` 등)
- [x] Clean Architecture 기반의 Activity 비의존적 연결 API 인터페이스 (`GlassConnectionApi`, `GlassConnectionEvent`)
- [x] 상태 전이, 속성 유효성, 에러 액션 매핑 단위 테스트 작성 (`GlassStateTest`)

### [x] 2단계: 가짜 안경(Mock) 어댑터 구현
- [x] 실기기 없이 개발/테스트 가능한 `MockGlassConnectionAdapter` 구현
- [x] 가상 연결/해제, 배터리 잔량 시뮬레이션, 임의 오류 발생 기능 제공
- [x] Mock 어댑터 단위 테스트 및 동작 검증 (`MockGlassConnectionAdapterTest`)

### [x] 3단계: AIGlassFood 기반 Meta DAT SDK 연동 어댑터 구현
- [x] `AIGlassFood`의 `WearablesViewModel` 및 디바이스 모니터링 로직을 이식한 `MetaGlassConnectionAdapter` 구현
- [x] 실제 Meta `DeviceSession` 수명주기, BLE/Wi-Fi 고속 데이터 링크 수립, 배터리/호환성 실시간 모니터링 연동
- [x] `AudioManager` 기반 실제 블루투스 오디오 출력 경로 실시간 감지 연동 및 Context 메모리 릭 방지

### [ ] 4단계: UI 화면 및 버튼 연동
- [ ] `GlassStatusBar` / 연결 상태 칩 및 배터리 표시 Composable UI 작성
- [ ] `InspectionViewModel`에서 `GlassConnectionApi` 상태 수집(`collectAsState`) 및 연결/해제 버튼 바인딩
- [ ] 에뮬레이터 및 실기기 연동 확인

---

## 📌 작업 기록 및 변경 이력

| 날짜 | 단계 | 내용 | 확인 방법 |
| :--- | :---: | :--- | :--- |
| 2026-08-14 | 계획 | 1~4단계 작업 계획 수립 및 AIGlassTask.md 작성 | 문서 등록 |
| 2026-08-14 | 1단계 | AI 글래스 연결 상태 API 및 도메인 모델 정의 완료 | 상태 모델 직교화 및 GlassStateTest 단위 테스트 통과 |
| 2026-08-14 | 2단계 | 가짜 안경(Mock) 어댑터 구현 완료 | MockGlassConnectionAdapterTest 단위 테스트 통과 |
| 2026-08-14 | 3단계 | Meta DAT SDK 기반 실제 기기 어댑터(MetaGlassConnectionAdapter) 구현 완료 | AIGlassFood 소스 이식 및 하드웨어 세션/오디오 감지 연동 |

