# Meta AI Glasses 기기 상태 재사용 가이드

다른 Android 프로젝트에 Meta AI Glasses의 **연결 상태**와 **배터리 경고 상태**를 넣을 때 사용하는 에이전트용 작업 기준이다. 기준 SDK는 이 저장소의 Meta Wearables DAT Android `0.9.0` 소스다. 실제 기기 검증 전에는 지원 여부를 확정하지 않는다.

## 1. 먼저 지켜야 할 경계

| 필요한 화면 정보 | DAT에서 사용할 근거 | 표시 규칙 |
| --- | --- | --- |
| 앱 등록 상태 | `Wearables.registrationState` | `REGISTERED`가 아니면 연결 시도 버튼 대신 등록 안내를 표시한다. |
| 사용 가능한 안경 | `Wearables.devices` | 목록에 기기가 있다고 해서 세션 연결 성공으로 표시하지 않는다. |
| 실제 앱-안경 세션 | `DeviceSession.state` | **`STARTED`일 때만 “연결됨”**으로 표시한다. `PAUSED`, `STOPPED`는 각각 별도 상태다. |
| 세션 장애 | `DeviceSession.errors` | 오류 코드를 UI와 로그에 남긴다. |
| 스트림 장애 | `Stream.errorStream` | 카메라 사용 중 `BATTERY_LOW` 등 상태를 경고한다. |
| 배터리 퍼센트 | 공개 DAT 0.9.0에서 확인되지 않음 | `75%` 같은 수치를 추정·표시하지 않는다. |

`BATTERY_LOW`(스트림), `BATTERY_CRITICAL`(세션)은 공개 오류 타입에 있다. 이는 **배터리 경고 신호**이지 잔량 조회 API가 아니다. `DeviceState`의 공개 실시간 값은 `thermalLevel`이다.

## 2. 다른 프로젝트에 넣을 최소 계약

UI와 ViewModel은 Meta SDK를 직접 알지 않고 아래 상태만 사용하게 만든다. 기존에 비슷한 상태 모델이 있으면 새 계약을 만들지 말고 그 모델에 이 필드만 추가한다.

```kotlin
enum class GlassConnectionStatus {
    NOT_REGISTERED,
    NO_DEVICE,
    CONNECTING,
    CONNECTED,
    PAUSED,
    DISCONNECTED,
    ERROR,
}

enum class GlassBatteryWarning { NONE, LOW, CRITICAL }

data class GlassDeviceStatus(
    val connection: GlassConnectionStatus = GlassConnectionStatus.NOT_REGISTERED,
    val batteryWarning: GlassBatteryWarning = GlassBatteryWarning.NONE,
    val thermalLevel: String? = null,
    val message: String? = null,
)
```

상태 매핑은 한 곳에서만 한다.

```text
registrationState != REGISTERED  -> NOT_REGISTERED
REGISTERED + devices.isEmpty()  -> NO_DEVICE
session STARTING                -> CONNECTING
session STARTED                 -> CONNECTED
session PAUSED                  -> PAUSED
session STOPPED                 -> DISCONNECTED
session/stream error            -> ERROR + 오류별 경고
```

## 3. 구현 순서

1. 대상 프로젝트에 이미 있는 `Application` 초기화 위치를 찾아 `Wearables.initialize(applicationContext)`를 한 번만 호출한다.
2. 등록 화면에서 `Wearables.startRegistration(activity)`를 호출하고 `registrationState`를 수집한다. 등록과 카메라 권한은 별도 흐름이다.
3. `Wearables.devices`를 수집해 기기 없음/발견 상태를 갱신한다.
4. 사용자가 연결을 요청하면 `Wearables.createSession(AutoDeviceSelector())`로 세션을 만들고, **`start()` 전에** `session.state`와 `session.errors` 수집을 시작한다.
5. `DeviceSessionState.STARTED`를 받은 뒤에만 `CONNECTED`로 전환하고 카메라 같은 capability를 붙인다.
6. `PAUSED`에서는 작업을 일시 중지하고 다음 상태를 기다린다. `STOPPED`에서는 collector와 capability를 해제하고, 같은 세션을 재시작하지 말고 다음 연결 때 새 세션을 만든다.
7. 카메라를 쓰는 프로젝트만 `Stream.errorStream`도 수집한다. `BATTERY_LOW`은 경고 배너, `BATTERY_CRITICAL`은 작업 중단 및 충전 안내로 매핑한다.
8. 필요하면 선택된 기기의 `Wearables.getDeviceState(deviceIdentifier)`에서 `thermalLevel`만 추가로 관찰한다. 배터리 퍼센트 필드를 만들거나 임의 계산하지 않는다.

## 4. 세션 관찰 최소 예시

```kotlin
private fun observeSession(session: DeviceSession) {
    viewModelScope.launch {
        session.state.collect { state ->
            status.value = status.value.copy(
                connection = when (state) {
                    DeviceSessionState.STARTING -> GlassConnectionStatus.CONNECTING
                    DeviceSessionState.STARTED -> GlassConnectionStatus.CONNECTED
                    DeviceSessionState.PAUSED -> GlassConnectionStatus.PAUSED
                    DeviceSessionState.STOPPED -> GlassConnectionStatus.DISCONNECTED
                    else -> status.value.connection
                },
            )
        }
    }
    viewModelScope.launch {
        session.errors.collect { error ->
            status.value = status.value.copy(
                connection = GlassConnectionStatus.ERROR,
                batteryWarning = if (error.name == "BATTERY_CRITICAL") {
                    GlassBatteryWarning.CRITICAL
                } else {
                    status.value.batteryWarning
                },
                message = error.description,
            )
        }
    }
}
```

에이전트는 SDK 오류 타입의 실제 enum 이름을 현재 설치된 DAT 버전에서 확인한 뒤 `error.name` 비교를 해당 타입의 `when` 분기로 교체한다. 이 예시는 상태 전파 위치를 보여 주기 위한 것이다.

## 5. 에이전트 작업 프롬프트

아래 문장을 대상 프로젝트의 구현 에이전트에게 전달한다.

```text
Meta Wearables DAT 0.9.0 기반으로 안경 기기 상태를 추가한다.

- 기존 상태 모델/연결 모듈을 먼저 찾아 재사용하고, UI가 Meta SDK 타입을 직접 참조하지 않게 한다.
- registrationState, devices, DeviceSession.state, DeviceSession.errors를 수집한다.
- session.state == STARTED일 때만 연결됨으로 표시한다. Bluetooth 연결 또는 devices 발견만으로 연결 성공 처리하지 않는다.
- 배터리 퍼센트 API는 가정하지 말고, StreamError.BATTERY_LOW과 DeviceSessionError.BATTERY_CRITICAL만 경고로 처리한다.
- PAUSED는 재연결 루프를 돌리지 말고 대기, STOPPED는 리소스 해제 후 다음 연결 때 새 세션을 생성한다.
- 실제 설치 DAT 버전의 API 심볼과 현재 Meta DAT 공식 문서를 확인하고, MockDeviceKit 검증과 실제 안경 검증을 분리해 보고한다.
- 변경 뒤에는 빌드와 상태 매핑 단위 테스트를 실행하고, 실제 안경에서 검증하지 못한 항목은 미검증으로 남긴다.
```

## 6. 완료 기준

- [ ] 등록 안 됨, 기기 없음, 연결 중, 연결됨, 일시 중지, 종료, 오류가 각각 화면에 구분된다.
- [ ] `STARTED` 전에는 연결됨 UI나 기능 버튼을 활성화하지 않는다.
- [ ] 배터리 숫자/퍼센트는 표시하지 않고 LOW/CRITICAL 경고만 표시한다.
- [ ] 세션 오류와 스트림 오류가 사용자 메시지와 개발 로그에 남는다.
- [ ] MockDeviceKit 결과와 실제 안경 결과를 별도로 기록한다.

## 7. 확인할 원본

- `meta-wearables-dat-android/AGENTS.md`: 현재 Android SDK 초기화·등록·세션 규칙
- `meta-wearables-dat-android/samples/CameraAccess/.../CameraViewModel.kt`: `start()` 전 상태/오류 collector 등록 예시
- `meta-wearables-dat-android/CHANGELOG.md`: `DeviceState.thermalLevel`, 배터리·열 오류 타입 추가 내역
- Meta DAT Android API reference: https://wearables.developer.meta.com/docs/reference/android/dat/latest
