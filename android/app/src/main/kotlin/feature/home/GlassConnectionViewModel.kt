package com.seipseip.app.feature.home

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import kotlinx.coroutines.Job
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

class GlassConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val selector = AutoDeviceSelector()
    private val _uiState = MutableStateFlow(GlassConnectionUiState())
    val uiState: StateFlow<GlassConnectionUiState> = _uiState.asStateFlow()
    private var session: DeviceSession? = null
    private var sessionJob: Job? = null

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
                    if (state == DeviceSessionState.STOPPED) { session = null; sessionJob?.cancel() }
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
}
