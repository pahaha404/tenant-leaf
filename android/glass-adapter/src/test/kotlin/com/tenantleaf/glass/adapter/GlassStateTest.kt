package com.tenantleaf.glass.adapter

import com.tenantleaf.glass.adapter.model.ErrorRecoveryAction
import com.tenantleaf.glass.adapter.model.GlassAudioRouteStatus
import com.tenantleaf.glass.adapter.model.GlassDeviceInfo
import com.tenantleaf.glass.adapter.model.GlassError
import com.tenantleaf.glass.adapter.model.GlassLinkStatus
import com.tenantleaf.glass.adapter.model.GlassRegistrationStatus
import com.tenantleaf.glass.adapter.model.GlassState
import com.tenantleaf.glass.adapter.model.GlassStreamStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GlassState 도메인 모델 단위 테스트")
class GlassStateTest {

    @Test
    @DisplayName("초기 기본 상태는 미등록 및 연결 해제 상태여야 한다")
    fun testDefaultStateProperties() {
        val state = GlassState()

        assertEquals(GlassRegistrationStatus.UNAVAILABLE, state.registration)
        assertEquals(GlassLinkStatus.DISCONNECTED, state.link)
        assertEquals(GlassStreamStatus.STOPPED, state.stream)
        assertEquals(GlassAudioRouteStatus.NOT_CONNECTED, state.audioRoute)
        assertNull(state.device)
        assertNull(state.unhandledError)

        assertFalse(state.isRegistered)
        assertFalse(state.isConnected)
        assertFalse(state.isStreaming)
        assertFalse(state.isReadyForInspection)
        assertFalse(state.isBusy)
        assertFalse(state.canPlayVoiceGuide)
    }

    @Test
    @DisplayName("등록 및 무선 링크가 연결되면 isReadyForInspection이 true여야 한다")
    fun testReadyForInspectionState() {
        val readyState = GlassState(
            registration = GlassRegistrationStatus.REGISTERED,
            link = GlassLinkStatus.CONNECTED,
            stream = GlassStreamStatus.STOPPED,
            device = GlassDeviceInfo(
                deviceId = "rayban-001",
                deviceName = "Ray-Ban Meta Shiny Black",
                batteryLevel = 85,
            ),
        )

        assertTrue(readyState.isRegistered)
        assertTrue(readyState.isConnected)
        assertTrue(readyState.isReadyForInspection)
        assertFalse(readyState.isStreaming)
        assertFalse(readyState.isBusy)
    }

    @Test
    @DisplayName("스트리밍 중일 때 isStreaming과 isConnected가 모두 true여야 한다")
    fun testStreamingState() {
        val streamingState = GlassState(
            registration = GlassRegistrationStatus.REGISTERED,
            link = GlassLinkStatus.CONNECTED,
            stream = GlassStreamStatus.STREAMING,
            device = GlassDeviceInfo(
                deviceId = "rayban-001",
                deviceName = "Ray-Ban Meta",
                batteryLevel = 75,
            ),
        )

        assertTrue(streamingState.isConnected)
        assertTrue(streamingState.isStreaming)
        assertTrue(streamingState.isReadyForInspection)
    }

    @Test
    @DisplayName("상태 전이가 진행 중일 때 isBusy는 true여야 한다")
    fun testBusyStateTransitions() {
        val connectingState = GlassState(
            registration = GlassRegistrationStatus.REGISTERED,
            link = GlassLinkStatus.CONNECTING,
        )
        assertTrue(connectingState.isBusy)

        val streamStartingState = GlassState(
            registration = GlassRegistrationStatus.REGISTERED,
            link = GlassLinkStatus.CONNECTED,
            stream = GlassStreamStatus.STARTING,
        )
        assertTrue(streamStartingState.isBusy)
        assertFalse(streamStartingState.isReadyForInspection)
    }

    @Test
    @DisplayName("배터리 잔량은 0..100 범위여야 하며 20퍼센트 이하는 isLowBattery가 true여야 한다")
    fun testBatteryValidation() {
        val lowBatteryDevice = GlassDeviceInfo(
            deviceId = "id-1",
            deviceName = "Glass",
            batteryLevel = 15,
        )
        assertTrue(lowBatteryDevice.isLowBattery)

        val normalBatteryDevice = GlassDeviceInfo(
            deviceId = "id-2",
            deviceName = "Glass",
            batteryLevel = 50,
        )
        assertFalse(normalBatteryDevice.isLowBattery)

        // 허용 범위를 벗어난 배터리 값 예외 검증
        assertThrows(IllegalArgumentException::class.java) {
            GlassDeviceInfo(deviceId = "err", deviceName = "Err", batteryLevel = 101)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GlassDeviceInfo(deviceId = "err", deviceName = "Err", batteryLevel = -5)
        }
    }

    @Test
    @DisplayName("에러 유형에 따라 적절한 UI 복구 액션이 매핑되어야 한다")
    fun testErrorRecoveryActionMapping() {
        val btError = GlassError.BluetoothDisabled
        assertEquals(ErrorRecoveryAction.OPEN_BLUETOOTH_SETTINGS, btError.recoveryAction)

        val permError = GlassError.PermissionDenied(listOf("android.permission.BLUETOOTH_CONNECT"))
        assertEquals(ErrorRecoveryAction.REQUEST_PERMISSIONS, permError.recoveryAction)

        val fwError = GlassError.FirmwareUpdateRequired("Ray-Ban Meta")
        assertEquals(ErrorRecoveryAction.OPEN_META_VIEW, fwError.recoveryAction)

        val timeoutError = GlassError.ConnectionTimeout
        assertEquals(ErrorRecoveryAction.RETRY, timeoutError.recoveryAction)
    }
}
