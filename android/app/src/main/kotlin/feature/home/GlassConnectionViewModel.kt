package com.seipseip.app.feature.home

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.view.Surface
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
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.seipseip.app.feature.inspection.preview.HevcDecoder
import com.seipseip.app.feature.inspection.preview.HevcParameterSetCollector
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GlassBatteryWarning { NONE, CRITICAL }

data class GlassConnectionUiState(
    val title: String = "Glass 등록 확인 필요",
    val detail: String = "탭하여 Meta AI 등록을 시작하세요",
    val connected: Boolean = false,
    val batteryWarning: GlassBatteryWarning = GlassBatteryWarning.NONE,
)

data class GlassPreviewUiState(
    val state: StreamState = StreamState.STOPPED,
    val hasFirstFrame: Boolean = false,
    val message: String? = null,
    val needsCameraPermission: Boolean = false,
) {
    val isActive: Boolean get() = state == StreamState.STARTING || state == StreamState.STREAMING || state == StreamState.PAUSED
    val isLive: Boolean get() = state == StreamState.STREAMING && hasFirstFrame
}

@OptIn(ExperimentalCoroutinesApi::class)
class GlassConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val selector = AutoDeviceSelector()
    private val _uiState = MutableStateFlow(GlassConnectionUiState())
    val uiState: StateFlow<GlassConnectionUiState> = _uiState.asStateFlow()
    private val _previewUiState = MutableStateFlow(GlassPreviewUiState())
    val previewUiState: StateFlow<GlassPreviewUiState> = _previewUiState.asStateFlow()
    private var session: DeviceSession? = null
    private var sessionJob: Job? = null
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
        viewModelScope.launch { Wearables.registrationState.collect { state ->
            if (state.name != "REGISTERED") _uiState.value = GlassConnectionUiState("Glass 등록 필요", "탭하여 Meta AI 등록을 시작하세요")
        } }
        viewModelScope.launch { selector.activeDeviceFlow().collect { device ->
            if (device != null && !(_uiState.value.connected)) _uiState.update { it.copy(title = "Glass 발견됨", detail = "탭하여 연결하세요") }
        } }
    }

    fun connect(activity: Activity) {
        if (Wearables.registrationState.value.name != "REGISTERED") { Wearables.startRegistration(activity); return }
        if (session != null) return
        Wearables.createSession(selector).onSuccess { created ->
            session = created
            sessionJob = viewModelScope.launch {
                created.state.collect { state ->
                    _uiState.value = when (state) {
                        DeviceSessionState.STARTING -> GlassConnectionUiState("Glass 연결 중", "안경과 세션을 시작하고 있어요")
                        DeviceSessionState.STARTED -> GlassConnectionUiState("세입세잎 Glass 연결됨", "촬영 기능을 준비할 수 있어요", true)
                        DeviceSessionState.PAUSED -> GlassConnectionUiState("Glass 일시 중지", "안경 상태가 돌아올 때까지 기다려요")
                        DeviceSessionState.STOPPED -> GlassConnectionUiState("Glass 연결 종료", "탭하여 다시 연결하세요")
                        else -> GlassConnectionUiState("Glass 연결 준비", "탭하여 연결하세요")
                    }
                    if (state == DeviceSessionState.STOPPED) {
                        clearPreview()
                        session = null
                        sessionJob?.cancel()
                    }
                }
            }
            viewModelScope.launch { created.errors.collect { error ->
                Log.e("TenantLeafDAT", error.description)
                val isBatteryCritical = error.toString().contains("BATTERY_CRITICAL")
                _uiState.value = if (isBatteryCritical) {
                    GlassConnectionUiState("Glass 배터리 부족", "안경을 충전한 뒤 다시 연결하세요", batteryWarning = GlassBatteryWarning.CRITICAL)
                } else {
                    GlassConnectionUiState("Glass 연결 오류", error.description)
                }
            } }
            created.start()
        }.onFailure { error, _ -> _uiState.value = GlassConnectionUiState("Glass 연결 오류", error.description) }
    }

    /** Compose owns the Surface; the decoder owns it only until it is destroyed. */
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
        if (stream != null || _previewUiState.value.state == StreamState.STARTING) return
        val activeSession = session
        if (activeSession == null || !_uiState.value.connected) {
            _previewUiState.value = GlassPreviewUiState(message = "안경 연결이 완료된 뒤 프리뷰를 시작할 수 있어요.")
            return
        }
        viewModelScope.launch {
            Wearables.checkPermissionStatus(Permission.CAMERA)
                .onSuccess { status ->
                    if (status == PermissionStatus.Granted) beginPreview(activeSession)
                    else _previewUiState.value = GlassPreviewUiState(needsCameraPermission = true, message = "Meta AI 앱에서 카메라 접근을 허용해 주세요.")
                }
                .onFailure { error, _ -> _previewUiState.value = GlassPreviewUiState(message = error.description) }
        }
    }

    fun onCameraPermissionResult(status: PermissionStatus) {
        _previewUiState.update { it.copy(needsCameraPermission = false) }
        if (status == PermissionStatus.Granted) session?.let(::beginPreview)
        else _previewUiState.value = GlassPreviewUiState(message = "안경 카메라 권한이 허용되지 않았어요.")
    }

    fun stopPreview() {
        _previewUiState.update { it.copy(state = StreamState.STOPPING, message = null) }
        camera?.close() ?: clearPreview()
    }

    private fun beginPreview(activeSession: DeviceSession) {
        if (stream != null) return
        activeSession.addCamera(
            StreamConfiguration(videoQuality = VideoQuality.MEDIUM, frameRate = 24, compressVideo = true),
        ).onSuccess { addedCamera ->
            camera = addedCamera
            val addedStream = addedCamera.stream
            stream = addedStream
            observePreview(addedStream) // Subscribe before start so STARTING and the first frame are not missed.
            _previewUiState.value = GlassPreviewUiState(state = StreamState.STARTING)
            addedStream.start().onFailure { error, _ ->
                _previewUiState.value = GlassPreviewUiState(message = error.description)
                clearPreview()
            }
        }.onFailure { error, _ -> _previewUiState.value = GlassPreviewUiState(message = error.description) }
    }

    private fun observePreview(activeStream: Stream) {
        previewVideoJob = viewModelScope.launch(previewDispatcher) { activeStream.videoStream.collect(::onVideoFrame) }
        previewStateJob = viewModelScope.launch {
            var wasActive = false
            activeStream.state.collect { state ->
                _previewUiState.update { it.copy(state = state, message = null) }
                if (state != StreamState.STOPPED && state != StreamState.CLOSED) wasActive = true
                else if (wasActive) clearPreview()
            }
        }
        previewErrorJob = viewModelScope.launch {
            activeStream.errorStream.collect { error ->
                Log.e("TenantLeafDAT", "Camera stream error: ${error.description}")
                _previewUiState.update { it.copy(message = error.description) }
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
        if (!frame.isCodecConfig && !_previewUiState.value.hasFirstFrame) {
            _previewUiState.update { it.copy(hasFirstFrame = true) }
        }
    }

    private fun clearPreview() {
        previewVideoJob?.cancel(); previewVideoJob = null
        previewStateJob?.cancel(); previewStateJob = null
        previewErrorJob?.cancel(); previewErrorJob = null
        synchronized(decoderLock) { decoder?.stop(); decoder = null }
        parameterSets.clear()
        camera?.close()
        camera = null
        stream = null
        _previewUiState.value = GlassPreviewUiState()
    }

    override fun onCleared() {
        clearPreview()
        session?.stop()
        super.onCleared()
    }
}
