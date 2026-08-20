package com.seipseip.app.feature.home

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.DeviceSessionError
import com.meta.wearable.dat.core.types.RegistrationState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal object GlassSessionReconnect {
    private val delaysMs = longArrayOf(1_000L, 3_000L, 6_000L)

    fun delayForAttempt(attempt: Int): Long? = delaysMs.getOrNull(attempt)
}

internal fun shouldReconnectAfter(error: DeviceSessionError): Boolean = error !in setOf(
    DeviceSessionError.BATTERY_CRITICAL,
    DeviceSessionError.THERMAL_CRITICAL,
    DeviceSessionError.THERMAL_EMERGENCY,
    DeviceSessionError.PEAK_POWER_SHUTDOWN,
    DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED,
)

enum class GlassConnectionStatus(
    val title: String,
    val detail: String,
) {
    NOT_REGISTERED("Meta Ray-Ban AI Glass · 등록 필요", "탭하여 Meta AI 등록을 시작하세요"),
    NO_DEVICE("Meta Ray-Ban AI Glass · 기기 없음", "Meta AI 앱에서 안경 연결을 확인하세요"),
    CONNECTING("Meta Ray-Ban AI Glass · 연결 중", "안경과 세션을 시작하고 있어요"),
    CONNECTED("Meta Ray-Ban AI Glass · 연결됨", "안경 세션이 연결되었어요"),
    PAUSED("Meta Ray-Ban AI Glass · 일시 중지", "안경 상태가 돌아올 때까지 기다려요"),
    DISCONNECTED("Meta Ray-Ban AI Glass · 연결 안 됨", "탭하여 다시 연결하세요"),
    ERROR("Meta Ray-Ban AI Glass · 연결 오류", "연결 상태를 다시 확인하세요"),
    ;

    companion object {
        fun fromSessionState(state: DeviceSessionState) = when (state) {
            DeviceSessionState.STARTING -> CONNECTING
            DeviceSessionState.STARTED -> CONNECTED
            DeviceSessionState.PAUSED -> PAUSED
            else -> DISCONNECTED
        }
    }
}

enum class GlassConnectionAction {
    REGISTER,
    START_SESSION,
    END_SESSION,
    NONE,
    ;

    companion object {
        fun nextFor(registrationState: RegistrationState, sessionState: DeviceSessionState?) = when {
            registrationState != RegistrationState.REGISTERED -> REGISTER
            sessionState == null -> START_SESSION
            sessionState == DeviceSessionState.STARTED || sessionState == DeviceSessionState.PAUSED -> END_SESSION
            else -> NONE
        }
    }
}

data class GlassConnectionUiState(
    val status: GlassConnectionStatus = GlassConnectionStatus.NOT_REGISTERED,
    val errorDetail: String? = null,
) {
    val title get() = status.title
    val detail get() = errorDetail ?: status.detail
}

class GlassConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(GlassConnectionUiState())
    val uiState: StateFlow<GlassConnectionUiState> = _uiState.asStateFlow()
    private val deviceSelector = AutoDeviceSelector()
    private var session: DeviceSession? = null
    private var sessionStateJob: Job? = null
    private var sessionErrorJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var stopRequested = false
    private var datAppUpdateRequired = false
    private var retryOnStop = true

    init {
        viewModelScope.launch {
            Wearables.registrationState.collect { registration ->
                when (registration) {
                    RegistrationState.REGISTERED -> if (session == null) update(GlassConnectionStatus.DISCONNECTED)
                    RegistrationState.REGISTERING -> update(GlassConnectionStatus.CONNECTING)
                    RegistrationState.AVAILABLE -> update(GlassConnectionStatus.NOT_REGISTERED)
                    RegistrationState.UNAVAILABLE -> update(GlassConnectionStatus.ERROR, "Meta AI 앱 사용 가능 여부를 확인하세요")
                    RegistrationState.UNREGISTERING -> update(GlassConnectionStatus.DISCONNECTED)
                }
            }
        }
        viewModelScope.launch {
            Wearables.registrationErrorStream.collect { error ->
                update(GlassConnectionStatus.ERROR, error.description)
            }
        }
        viewModelScope.launch {
            Wearables.devices.collect { devices ->
                if (session == null && Wearables.registrationState.value == RegistrationState.REGISTERED && devices.isEmpty()) {
                    update(GlassConnectionStatus.NO_DEVICE)
                }
            }
        }
    }

    fun connect(activity: Activity) {
        reconnectJob?.cancel()
        reconnectJob = null
        if (datAppUpdateRequired) {
            Wearables.openDATGlassesAppUpdate(activity).onFailure { error, _ ->
                update(GlassConnectionStatus.ERROR, error.description)
            }
            return
        }
        when (GlassConnectionAction.nextFor(Wearables.registrationState.value, session?.state?.value)) {
            GlassConnectionAction.REGISTER -> Wearables.startRegistration(activity)
            GlassConnectionAction.START_SESSION -> {
                reconnectAttempt = 0
                startSession()
            }
            GlassConnectionAction.END_SESSION -> {
                stopRequested = true
                session?.stop()
            }
            GlassConnectionAction.NONE -> Unit
        }
    }

    private fun startSession() {
        if (session != null) return
        update(GlassConnectionStatus.CONNECTING)
        Wearables.createSession(deviceSelector).fold(
            onSuccess = { created ->
                session = created
                observe(created)
                created.start()
            },
            onFailure = { error, _ ->
                handleSessionError(error)
                cleanupSession()
                scheduleReconnect()
            },
        )
    }

    private fun observe(created: DeviceSession) {
        sessionStateJob = viewModelScope.launch {
            created.state.collect { state ->
                update(GlassConnectionStatus.fromSessionState(state))
                if (state == DeviceSessionState.STARTED) {
                    reconnectAttempt = 0
                }
                if (state == DeviceSessionState.STOPPED) {
                    cleanupSession()
                    scheduleReconnect()
                }
            }
        }
        sessionErrorJob = viewModelScope.launch {
            created.errors.collect { error ->
                handleSessionError(error)
            }
        }
    }

    private fun cleanupSession() {
        sessionStateJob?.cancel()
        sessionStateJob = null
        sessionErrorJob?.cancel()
        sessionErrorJob = null
        session = null
    }

    private fun scheduleReconnect() {
        if (stopRequested || datAppUpdateRequired || !retryOnStop) {
            stopRequested = false
            return
        }
        val delayMs = GlassSessionReconnect.delayForAttempt(reconnectAttempt) ?: return
        reconnectAttempt += 1
        update(
            GlassConnectionStatus.DISCONNECTED,
            "연결이 끊겨 ${delayMs / 1_000}초 뒤 다시 연결합니다",
        )
        reconnectJob = viewModelScope.launch {
            delay(delayMs)
            if (Wearables.registrationState.value == RegistrationState.REGISTERED) {
                startSession()
            }
        }
    }

    private fun update(status: GlassConnectionStatus, errorDetail: String? = null) {
        _uiState.value = GlassConnectionUiState(status, errorDetail)
    }

    private fun handleSessionError(error: DeviceSessionError) {
        retryOnStop = shouldReconnectAfter(error)
        datAppUpdateRequired = error == DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED
        update(
            GlassConnectionStatus.ERROR,
            if (datAppUpdateRequired) "안경 DAT 앱 업데이트가 필요합니다. 탭하여 업데이트하세요." else error.description,
        )
    }

    override fun onCleared() {
        reconnectJob?.cancel()
        stopRequested = true
        session?.stop()
        cleanupSession()
        super.onCleared()
    }
}
