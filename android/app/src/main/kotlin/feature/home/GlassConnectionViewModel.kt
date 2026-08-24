package com.seipseip.app.feature.home

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.Stream
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.camera.types.VideoFrame
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.DeviceSessionError
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.core.types.RegistrationState
import com.seipseip.app.feature.inspection.preview.HevcDecoder
import com.seipseip.app.feature.inspection.preview.HevcParameterSetCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberGlassConnectionViewModel(): GlassConnectionViewModel {
    val context = LocalContext.current
    val activity = context.findActivity()
    return if (activity != null) {
        viewModel(viewModelStoreOwner = activity)
    } else {
        viewModel()
    }
}

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
    val isConnected get() = status == GlassConnectionStatus.CONNECTED
}

data class GlassPreviewUiState(
    val state: StreamState = StreamState.STOPPED,
    val hasFirstFrame: Boolean = false,
    val needsCameraPermission: Boolean = false,
    val message: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
) {
    val isStreaming get() = state == StreamState.STREAMING
    val isStarting get() = state == StreamState.STARTING
}

class GlassConnectionViewModel(application: Application) : AndroidViewModel(application) {
    val uiState: StateFlow<GlassConnectionUiState> get() = Companion._sharedUiState.asStateFlow()
    val previewUiState: StateFlow<GlassPreviewUiState> get() = Companion._sharedPreviewUiState.asStateFlow()

    companion object {
        private const val TAG = "TenantLeafDAT"
        private val _sharedUiState = MutableStateFlow(GlassConnectionUiState())
        private val _sharedPreviewUiState = MutableStateFlow(GlassPreviewUiState())
        private val deviceSelector = AutoDeviceSelector()
        private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private var session: DeviceSession? = null
        private var sessionStateJob: Job? = null
        private var sessionErrorJob: Job? = null
        private var reconnectJob: Job? = null
        private var reconnectAttempt = 0
        private var stopRequested = false
        private var datAppUpdateRequired = false
        private var retryOnStop = true

        private var camera: Camera? = null
        private var stream: Stream? = null
        private var previewVideoJob: Job? = null
        private var previewStateJob: Job? = null
        private var previewErrorJob: Job? = null
        private val previewDispatcher = Dispatchers.Default.limitedParallelism(1)
        private val decoderLock = Any()
        private val parameterSets = HevcParameterSetCollector()
        @Volatile private var previewSurface: Surface? = null
        @Volatile private var decoder: HevcDecoder? = null

        init {
            sessionScope.launch {
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
            sessionScope.launch {
                Wearables.registrationErrorStream.collect { error ->
                    update(GlassConnectionStatus.ERROR, error.description)
                }
            }
            sessionScope.launch {
                Wearables.devices.collect { devices ->
                    if (session == null && Wearables.registrationState.value == RegistrationState.REGISTERED && devices.isEmpty()) {
                        update(GlassConnectionStatus.NO_DEVICE)
                    }
                }
            }
        }

        fun setPreviewSurface(surface: Surface?) {
            synchronized(decoderLock) {
                previewSurface = surface
                if (surface == null) {
                    decoder?.stop()
                    decoder = null
                }
            }
        }

        fun startPreview() {
            if (stream != null || _sharedPreviewUiState.value.state == StreamState.STARTING) return
            val activeSession = session
            if (activeSession == null || !_sharedUiState.value.isConnected) {
                _sharedPreviewUiState.value = GlassPreviewUiState(message = "안경 연결이 완료된 뒤 프리뷰를 시작할 수 있어요.")
                return
            }
            sessionScope.launch {
                Wearables.checkPermissionStatus(Permission.CAMERA).fold(
                    onSuccess = { status ->
                        if (status == PermissionStatus.Granted) {
                            beginPreview(activeSession)
                        } else {
                            _sharedPreviewUiState.value = GlassPreviewUiState(
                                needsCameraPermission = true,
                                message = "Meta AI 앱에서 카메라 접근을 허용해 주세요.",
                            )
                        }
                    },
                    onFailure = { error, _ ->
                        _sharedPreviewUiState.value = GlassPreviewUiState(message = error.description)
                    },
                )
            }
        }

        fun stopPreview() {
            _sharedPreviewUiState.update { it.copy(state = StreamState.STOPPING, message = null) }
            camera?.close() ?: clearPreview()
        }

        private fun beginPreview(activeSession: DeviceSession) {
            if (stream != null) return
            activeSession.addCamera(
                StreamConfiguration(videoQuality = VideoQuality.HIGH, frameRate = 30, compressVideo = true),
            ).fold(
                onSuccess = { addedCamera ->
                    camera = addedCamera
                    val addedStream = addedCamera.stream
                    stream = addedStream
                    observePreview(addedStream)
                    _sharedPreviewUiState.value = GlassPreviewUiState(state = StreamState.STARTING)
                    addedStream.start().onFailure { error, _ ->
                        _sharedPreviewUiState.value = GlassPreviewUiState(message = error.description)
                        clearPreview()
                    }
                },
                onFailure = { error, _ ->
                    _sharedPreviewUiState.value = GlassPreviewUiState(message = error.description)
                },
            )
        }

        private fun observePreview(activeStream: Stream) {
            previewVideoJob = sessionScope.launch(previewDispatcher) {
                activeStream.videoStream.collect(::onVideoFrame)
            }
            previewStateJob = sessionScope.launch {
                var wasActive = false
                activeStream.state.collect { state ->
                    _sharedPreviewUiState.update { it.copy(state = state, message = null) }
                    if (state != StreamState.STOPPED && state != StreamState.CLOSED) {
                        wasActive = true
                    } else if (wasActive) {
                        clearPreview()
                    }
                }
            }
            previewErrorJob = sessionScope.launch {
                activeStream.errorStream.collect { error ->
                    Log.e(TAG, "Camera stream error: ${error.description}")
                    _sharedPreviewUiState.update { it.copy(message = error.description) }
                }
            }
        }

        private fun onVideoFrame(frame: VideoFrame) {
            if (!frame.isCompressed) return
            val buffer = frame.buffer
            val bytes = ByteArray(buffer.remaining())
            val position = buffer.position()
            buffer.get(bytes)
            buffer.position(position)
            parameterSets.offer(bytes)
            synchronized(decoderLock) {
                val surface = previewSurface
                if (decoder == null && surface != null) {
                    decoder = HevcDecoder().also {
                        it.start(frame.width, frame.height, surface)
                        parameterSets.complete()?.let { config -> it.decodeFrame(config, 0) }
                    }
                }
                decoder?.decodeFrame(bytes, frame.presentationTimeUs)
            }
            if (!frame.isCodecConfig && (!_sharedPreviewUiState.value.hasFirstFrame || _sharedPreviewUiState.value.videoWidth != frame.width || _sharedPreviewUiState.value.videoHeight != frame.height)) {
                _sharedPreviewUiState.update {
                    it.copy(
                        hasFirstFrame = true,
                        videoWidth = frame.width,
                        videoHeight = frame.height,
                    )
                }
            }
        }

        private fun clearPreview() {
            previewVideoJob?.cancel()
            previewVideoJob = null
            previewStateJob?.cancel()
            previewStateJob = null
            previewErrorJob?.cancel()
            previewErrorJob = null
            synchronized(decoderLock) {
                decoder?.stop()
                decoder = null
            }
            parameterSets.clear()
            camera?.close()
            camera = null
            stream = null
            _sharedPreviewUiState.value = GlassPreviewUiState()
        }

        private fun update(status: GlassConnectionStatus, errorDetail: String? = null) {
            _sharedUiState.value = GlassConnectionUiState(status, errorDetail)
        }
    }

    fun connect(activity: Activity) {
        Companion.reconnectJob?.cancel()
        Companion.reconnectJob = null
        if (Companion.datAppUpdateRequired) {
            Wearables.openDATGlassesAppUpdate(activity).onFailure { error, _ ->
                Companion.update(GlassConnectionStatus.ERROR, error.description)
            }
            return
        }
        when (GlassConnectionAction.nextFor(Wearables.registrationState.value, Companion.session?.state?.value)) {
            GlassConnectionAction.REGISTER -> Wearables.startRegistration(activity)
            GlassConnectionAction.START_SESSION -> {
                Companion.reconnectAttempt = 0
                startSession()
            }
            GlassConnectionAction.END_SESSION -> {
                Companion.stopRequested = true
                Companion.session?.stop()
            }
            GlassConnectionAction.NONE -> Unit
        }
    }

    fun setPreviewSurface(surface: Surface?) = Companion.setPreviewSurface(surface)
    fun startPreview() = Companion.startPreview()
    fun stopPreview() = Companion.stopPreview()

    private fun startSession() {
        if (Companion.session != null) return
        Companion.update(GlassConnectionStatus.CONNECTING)
        Wearables.createSession(Companion.deviceSelector).fold(
            onSuccess = { created ->
                Companion.session = created
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
        Companion.sessionStateJob = Companion.sessionScope.launch {
            created.state.collect { state ->
                Companion.update(GlassConnectionStatus.fromSessionState(state))
                if (state == DeviceSessionState.STARTED) {
                    Companion.reconnectAttempt = 0
                }
                if (state == DeviceSessionState.STOPPED) {
                    Companion.clearPreview()
                    cleanupSession()
                    scheduleReconnect()
                }
            }
        }
        Companion.sessionErrorJob = Companion.sessionScope.launch {
            created.errors.collect { error ->
                handleSessionError(error)
            }
        }
    }

    private fun cleanupSession() {
        Companion.sessionStateJob?.cancel()
        Companion.sessionStateJob = null
        Companion.sessionErrorJob?.cancel()
        Companion.sessionErrorJob = null
        Companion.session = null
    }

    private fun scheduleReconnect() {
        if (Companion.stopRequested || Companion.datAppUpdateRequired || !Companion.retryOnStop) {
            Companion.stopRequested = false
            return
        }
        val delayMs = GlassSessionReconnect.delayForAttempt(Companion.reconnectAttempt) ?: return
        Companion.reconnectAttempt += 1
        Companion.update(
            GlassConnectionStatus.DISCONNECTED,
            "연결이 끊겨 ${delayMs / 1_000}초 뒤 다시 연결합니다",
        )
        Companion.reconnectJob = Companion.sessionScope.launch {
            delay(delayMs)
            if (Wearables.registrationState.value == RegistrationState.REGISTERED) {
                startSession()
            }
        }
    }

    private fun handleSessionError(error: DeviceSessionError) {
        Companion.retryOnStop = shouldReconnectAfter(error)
        Companion.datAppUpdateRequired = error == DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED
        Companion.update(
            GlassConnectionStatus.ERROR,
            if (Companion.datAppUpdateRequired) "안경 DAT 앱 업데이트가 필요합니다. 탭하여 업데이트하세요." else error.description,
        )
    }

    override fun onCleared() {
        cleanupSession()
        super.onCleared()
    }
}

