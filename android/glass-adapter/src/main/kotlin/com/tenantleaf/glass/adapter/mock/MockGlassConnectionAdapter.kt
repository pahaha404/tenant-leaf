package com.tenantleaf.glass.adapter.mock

import com.tenantleaf.glass.adapter.GlassConnectionApi
import com.tenantleaf.glass.adapter.GlassConnectionEvent
import com.tenantleaf.glass.adapter.model.GlassAudioRouteStatus
import com.tenantleaf.glass.adapter.model.GlassDeviceInfo
import com.tenantleaf.glass.adapter.model.GlassError
import com.tenantleaf.glass.adapter.model.GlassLinkStatus
import com.tenantleaf.glass.adapter.model.GlassRegistrationStatus
import com.tenantleaf.glass.adapter.model.GlassState
import com.tenantleaf.glass.adapter.model.GlassStreamStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 실기기 안경 없이 UI 개발 및 로직 테스트를 수행할 수 있는 가상(Mock) 글래스 어댑터.
 * Mutex 기반의 동시성 제어, 취소 안전성(Cancellation Safety), 멀티 컬렉터 지원 SharedFlow를 탑재하여
 * 실제 하드웨어의 비동기 동작과 엣지 케이스를 안전하게 시뮬레이션합니다.
 */
class MockGlassConnectionAdapter(
    initialState: GlassState = GlassState(
        registration = GlassRegistrationStatus.REGISTERED, // 테스트 편의상 기본 등록 완료
        link = GlassLinkStatus.DISCONNECTED,
    ),
    private val simulatedDelayMs: Long = 100L, // 단위 테스트 시 빠른 완료를 위해 기본 100ms
) : GlassConnectionApi {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<GlassState> = _state.asStateFlow()

    // 다중 구독자를 안전하게 지원하고 이벤트 유실을 방지하는 버퍼 SharedFlow
    private val _events = MutableSharedFlow<GlassConnectionEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<GlassConnectionEvent> = _events.asSharedFlow()

    // 동시 호출 방지 및 원자적 트랜잭션을 보장하는 락
    private val actionMutex = Mutex()

    // 가상 기기 등록(Pairing) 요청
    override suspend fun requestRegistration(): Unit = actionMutex.withLock {
        _events.tryEmit(GlassConnectionEvent.LaunchRegistrationFlow)
        _state.update { it.copy(registration = GlassRegistrationStatus.REGISTERING) }
        try {
            if (simulatedDelayMs > 0) delay(simulatedDelayMs)
            _state.update { it.copy(registration = GlassRegistrationStatus.REGISTERED) }
        } catch (e: CancellationException) {
            _state.update { it.copy(registration = GlassRegistrationStatus.UNREGISTERED) }
            throw e
        }
    }

    // 가상 기기 등록 해제 요청
    override suspend fun requestUnregistration(): Unit = actionMutex.withLock {
        _events.tryEmit(GlassConnectionEvent.LaunchUnregistrationFlow)
        _state.update { it.copy(registration = GlassRegistrationStatus.UNREGISTERING) }
        try {
            if (simulatedDelayMs > 0) delay(simulatedDelayMs)
            _state.update {
                it.copy(
                    registration = GlassRegistrationStatus.UNREGISTERED,
                    link = GlassLinkStatus.DISCONNECTED,
                    stream = GlassStreamStatus.STOPPED,
                    audioRoute = GlassAudioRouteStatus.NOT_CONNECTED,
                    device = null,
                )
            }
            _events.tryEmit(GlassConnectionEvent.Disconnected)
        } catch (e: CancellationException) {
            _state.update { it.copy(registration = GlassRegistrationStatus.REGISTERED) }
            throw e
        }
    }

    // 가상 안경 무선 링크 연결 및 세션 시작
    override suspend fun connect(): Result<Unit> = actionMutex.withLock {
        if (_state.value.registration != GlassRegistrationStatus.REGISTERED) {
            val error = GlassError.DeviceNotFound
            _state.update { it.copy(unhandledError = error) }
            _events.tryEmit(GlassConnectionEvent.ErrorOccurred(error))
            return Result.failure(IllegalStateException(error.userMessage))
        }

        _state.update { it.copy(link = GlassLinkStatus.CONNECTING, unhandledError = null) }
        try {
            if (simulatedDelayMs > 0) delay(simulatedDelayMs)

            val mockDevice = GlassDeviceInfo(
                deviceId = "mock-rayban-001",
                deviceName = "Mock Ray-Ban Meta",
                batteryLevel = 90,
                isCharging = false,
                isFirmwareUpdateRequired = false,
            )

            _state.update {
                it.copy(
                    link = GlassLinkStatus.CONNECTED,
                    device = mockDevice,
                    audioRoute = GlassAudioRouteStatus.RAYBAN_SPEAKER_ACTIVE,
                    stream = GlassStreamStatus.STOPPED,
                )
            }
            _events.tryEmit(GlassConnectionEvent.Connected(mockDevice.deviceName))
            Result.success(Unit)
        } catch (e: CancellationException) {
            _state.update { it.copy(link = GlassLinkStatus.DISCONNECTED) }
            throw e
        }
    }

    // 가상 안경 무선 연결 해제
    override suspend fun disconnect(): Unit = actionMutex.withLock {
        _state.update { it.copy(link = GlassLinkStatus.DISCONNECTING) }
        try {
            if (simulatedDelayMs > 0) delay(simulatedDelayMs)
            _state.update {
                it.copy(
                    link = GlassLinkStatus.DISCONNECTED,
                    stream = GlassStreamStatus.STOPPED,
                    audioRoute = GlassAudioRouteStatus.NOT_CONNECTED,
                    device = null,
                )
            }
            _events.tryEmit(GlassConnectionEvent.Disconnected)
        } catch (e: CancellationException) {
            _state.update { it.copy(link = GlassLinkStatus.DISCONNECTED) }
            throw e
        }
    }

    // 최근 에러 상태 초기화
    override fun clearError() {
        _state.update { it.copy(unhandledError = null) }
    }

    // ==========================================
    // 🧪 테스트 및 디버그용 상태 조작 시뮬레이터 함수들
    // ==========================================

    // 가상 배터리 잔량 및 충전 상태 변경
    fun simulateBattery(level: Int, isCharging: Boolean = false) {
        val currentDevice = _state.value.device ?: GlassDeviceInfo(
            deviceId = "mock-rayban-001",
            deviceName = "Mock Ray-Ban Meta",
        )
        _state.update { it.copy(device = currentDevice.copy(batteryLevel = level, isCharging = isCharging)) }
    }

    // 가상 에러 발생 동기식 주입
    fun simulateError(error: GlassError) {
        _state.update { it.copy(unhandledError = error) }
        _events.tryEmit(GlassConnectionEvent.ErrorOccurred(error))
    }

    // 가상 스트리밍 상태 변경
    fun simulateStreaming(isStreaming: Boolean) {
        _state.update {
            it.copy(stream = if (isStreaming) GlassStreamStatus.STREAMING else GlassStreamStatus.STOPPED)
        }
    }

    // 가상 오디오 출력 경로 변경
    fun simulateAudioRoute(route: GlassAudioRouteStatus) {
        _state.update { it.copy(audioRoute = route) }
    }

    // 가상 비정상 연결 끊김 시뮬레이션
    fun simulateDeviceDrop() {
        _state.update {
            it.copy(
                link = GlassLinkStatus.DISCONNECTED,
                stream = GlassStreamStatus.STOPPED,
                audioRoute = GlassAudioRouteStatus.NOT_CONNECTED,
                device = null,
                unhandledError = GlassError.ConnectionTimeout,
            )
        }
        _events.tryEmit(GlassConnectionEvent.Disconnected)
        _events.tryEmit(GlassConnectionEvent.ErrorOccurred(GlassError.ConnectionTimeout))
    }
}
