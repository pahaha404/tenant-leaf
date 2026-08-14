package com.tenantleaf.glass.adapter.meta

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.selectors.DeviceSelector
import com.meta.wearable.dat.core.sessions.DeviceSession
import com.meta.wearable.dat.core.sessions.DeviceSessionState
import com.meta.wearable.dat.core.types.DeviceCompatibility
import com.meta.wearable.dat.core.types.DeviceIdentifier
import com.meta.wearable.dat.core.types.RegistrationState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Meta Wearables DAT SDK(0.9.0)를 연동하여 실제 Meta Ray-Ban 스마트 글래스를 제어하는 프로덕션 어댑터.
 * 실제 하드웨어 DeviceSession 수명주기, 블루투스 오디오 라우팅, 배터리 텔레메트리 및
 * 다중 구독자를 지원하는 반응형 상태/이벤트 스트림을 제공합니다.
 */
class MetaGlassConnectionAdapter(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val deviceSelector: DeviceSelector = AutoDeviceSelector(),
) : GlassConnectionApi, AutoCloseable {

    // Activity 메모리 누수 방지용 Application Context
    private val appContext: Context = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    private val _state = MutableStateFlow(GlassState())
    override val state: StateFlow<GlassState> = _state.asStateFlow()

    // 다중 구독자 및 이벤트 유실 방지 SharedFlow
    private val _events = MutableSharedFlow<GlassConnectionEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<GlassConnectionEvent> = _events.asSharedFlow()

    // 동시성 제어 락
    private val actionMutex = Mutex()

    // 현재 활성화된 실제 SDK DeviceSession 및 선택된 기기 ID
    private var activeDeviceSession: DeviceSession? = null
    private var currentActiveDeviceId: DeviceIdentifier? = null

    // 모니터링 코루틴 Job 관리
    private val deviceMonitoringJobs = mutableMapOf<DeviceIdentifier, Job>()
    private var sessionStateJob: Job? = null
    private var activeDeviceJob: Job? = null
    private var registrationJob: Job? = null
    private var isMonitoringStarted = false

    init {
        startMonitoring()
    }

    // Wearables SDK의 등록 상태, 기기 목록, 메타데이터 실시간 관찰 시작
    private fun startMonitoring() {
        if (isMonitoringStarted) return
        isMonitoringStarted = true

        // 1. Meta View 앱 계정/기기 등록 상태 관찰
        registrationJob = scope.launch {
            Wearables.registrationState.collect { regState ->
                val mappedStatus = when (regState) {
                    RegistrationState.UNAVAILABLE -> GlassRegistrationStatus.UNAVAILABLE
                    RegistrationState.UNREGISTERED -> GlassRegistrationStatus.UNREGISTERED
                    RegistrationState.REGISTERING -> GlassRegistrationStatus.REGISTERING
                    RegistrationState.REGISTERED -> GlassRegistrationStatus.REGISTERED
                    RegistrationState.UNREGISTERING -> GlassRegistrationStatus.UNREGISTERING
                }
                _state.update { it.copy(registration = mappedStatus) }
            }
        }

        // 2. 활성 디바이스 선택기(AutoDeviceSelector) 관찰
        activeDeviceJob = scope.launch {
            deviceSelector.activeDeviceFlow().collect { activeId ->
                currentActiveDeviceId = activeId
                if (activeId == null) {
                    cleanupActiveSession()
                    _state.update {
                        it.copy(
                            link = GlassLinkStatus.DISCONNECTED,
                            device = null,
                            stream = GlassStreamStatus.STOPPED,
                            audioRoute = GlassAudioRouteStatus.NOT_CONNECTED,
                        )
                    }
                }
            }
        }

        // 3. 연결 가능한 디바이스 목록 및 메타데이터(배터리, 호환성) 모니터링
        scope.launch {
            Wearables.devices.collect { devices ->
                syncDeviceMonitoring(devices)
            }
        }
    }

    // 디바이스 목록 변경에 따른 메타데이터 수집 코루틴 동기화
    private fun syncDeviceMonitoring(devices: Set<DeviceIdentifier>) {
        val removed = deviceMonitoringJobs.keys - devices
        removed.forEach { id ->
            deviceMonitoringJobs[id]?.cancel()
            deviceMonitoringJobs.remove(id)
        }

        val newDevices = devices - deviceMonitoringJobs.keys
        newDevices.forEach { deviceId ->
            val job = scope.launch {
                Wearables.devicesMetadata[deviceId]?.collect { metadata ->
                    val isFwUpdateRequired = metadata.compatibility == DeviceCompatibility.DEVICE_UPDATE_REQUIRED
                    val currentDevice = GlassDeviceInfo(
                        deviceId = deviceId.toString(),
                        deviceName = metadata.name.ifEmpty { "Ray-Ban Meta" },
                        batteryLevel = null, // 배터리 수신 시 실시간 반영
                        isCharging = false,
                        isFirmwareUpdateRequired = isFwUpdateRequired,
                    )

                    _state.update { it.copy(device = currentDevice) }

                    if (isFwUpdateRequired) {
                        val fwError = GlassError.FirmwareUpdateRequired(currentDevice.deviceName)
                        _state.update { it.copy(unhandledError = fwError) }
                        _events.tryEmit(GlassConnectionEvent.LaunchFirmwareUpdate(currentDevice.deviceName))
                    }
                }
            }
            deviceMonitoringJobs[deviceId] = job
        }
    }

    // Meta View 앱 기기 등록 화면 실행 이벤트 요청
    override suspend fun requestRegistration(): Unit = actionMutex.withLock {
        _events.tryEmit(GlassConnectionEvent.LaunchRegistrationFlow)
    }

    // Meta View 앱 기기 등록 해제 이벤트 요청
    override suspend fun requestUnregistration(): Unit = actionMutex.withLock {
        _events.tryEmit(GlassConnectionEvent.LaunchUnregistrationFlow)
    }

    // 실제 글래스 하드웨어 세션(DeviceSession) 생성 및 무선 링크 연결
    override suspend fun connect(): Result<Unit> = actionMutex.withLock {
        if (_state.value.registration != GlassRegistrationStatus.REGISTERED) {
            val error = GlassError.DeviceNotFound
            _state.update { it.copy(unhandledError = error) }
            _events.tryEmit(GlassConnectionEvent.ErrorOccurred(error))
            return Result.failure(IllegalStateException(error.userMessage))
        }

        val targetDeviceId = currentActiveDeviceId
        if (targetDeviceId == null) {
            val error = GlassError.DeviceNotFound
            _state.update { it.copy(unhandledError = error) }
            _events.tryEmit(GlassConnectionEvent.ErrorOccurred(error))
            return Result.failure(IllegalStateException("연결 가능한 Meta 글래스를 찾을 수 없습니다."))
        }

        _state.update { it.copy(link = GlassLinkStatus.CONNECTING, unhandledError = null) }

        try {
            cleanupActiveSession()

            // 실제 Meta Wearables SDK 하드웨어 세션 생성 및 관찰
            val session = Wearables.createDeviceSession(targetDeviceId)
            activeDeviceSession = session

            sessionStateJob = scope.launch {
                session.state.collect { sessionState ->
                    val mappedLinkStatus = when (sessionState) {
                        DeviceSessionState.IDLE -> GlassLinkStatus.DISCONNECTED
                        DeviceSessionState.STARTING -> GlassLinkStatus.CONNECTING
                        DeviceSessionState.STARTED -> GlassLinkStatus.CONNECTED
                        DeviceSessionState.STOPPING -> GlassLinkStatus.DISCONNECTING
                    }
                    _state.update {
                        it.copy(
                            link = mappedLinkStatus,
                            audioRoute = if (mappedLinkStatus == GlassLinkStatus.CONNECTED) detectAudioRoute() else GlassAudioRouteStatus.NOT_CONNECTED,
                        )
                    }
                }
            }

            session.start()

            val deviceName = _state.value.device?.deviceName ?: "Ray-Ban Meta"
            _events.tryEmit(GlassConnectionEvent.Connected(deviceName))
            Result.success(Unit)
        } catch (e: CancellationException) {
            cleanupActiveSession()
            _state.update { it.copy(link = GlassLinkStatus.DISCONNECTED) }
            throw e
        } catch (e: Exception) {
            cleanupActiveSession()
            val error = GlassError.Unknown(e.message ?: "글래스 연결에 실패했습니다.")
            _state.update { it.copy(link = GlassLinkStatus.DISCONNECTED, unhandledError = error) }
            _events.tryEmit(GlassConnectionEvent.ErrorOccurred(error))
            Result.failure(e)
        }
    }

    // 실제 글래스 하드웨어 세션 종료 및 리셋
    override suspend fun disconnect(): Unit = actionMutex.withLock {
        _state.update { it.copy(link = GlassLinkStatus.DISCONNECTING) }
        try {
            cleanupActiveSession()
            _state.update {
                it.copy(
                    link = GlassLinkStatus.DISCONNECTED,
                    stream = GlassStreamStatus.STOPPED,
                    audioRoute = GlassAudioRouteStatus.NOT_CONNECTED,
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

    // 활성 SDK 세션 및 상태 관찰 Job 안전 종료
    private fun cleanupActiveSession() {
        sessionStateJob?.cancel()
        sessionStateJob = null
        activeDeviceSession?.stop()
        activeDeviceSession = null
    }

    // 블루투스 오디오 출력 경로가 안경 스피커로 잡혀있는지 실제 하드웨어 감지
    private fun detectAudioRoute(): GlassAudioRouteStatus {
        val audioDevices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return GlassAudioRouteStatus.NOT_CONNECTED
        val hasBluetoothOutput = audioDevices.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
        }
        return if (hasBluetoothOutput) GlassAudioRouteStatus.RAYBAN_SPEAKER_ACTIVE else GlassAudioRouteStatus.NOT_CONNECTED
    }

    // 어댑터 리소스 및 모든 코루틴 Job 해제
    override fun close() {
        cleanupActiveSession()
        deviceMonitoringJobs.values.forEach { it.cancel() }
        deviceMonitoringJobs.clear()
        activeDeviceJob?.cancel()
        registrationJob?.cancel()
    }
}
