package com.tenantleaf.glass.adapter.mock

import com.tenantleaf.glass.adapter.GlassConnectionEvent
import com.tenantleaf.glass.adapter.model.GlassAudioRouteStatus
import com.tenantleaf.glass.adapter.model.GlassError
import com.tenantleaf.glass.adapter.model.GlassLinkStatus
import com.tenantleaf.glass.adapter.model.GlassRegistrationStatus
import com.tenantleaf.glass.adapter.model.GlassState
import com.tenantleaf.glass.adapter.model.GlassStreamStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("MockGlassConnectionAdapter Best Practices 검증 테스트")
class MockGlassConnectionAdapterTest {

    @Test
    @DisplayName("등록 상태에서 connect 호출 시 연결 성공 및 상태가 CONNECTED로 변경되어야 한다")
    fun testSuccessfulConnection() = runTest {
        val adapter = MockGlassConnectionAdapter(simulatedDelayMs = 0L)

        val result = adapter.connect()
        assertTrue(result.isSuccess)

        val currentState = adapter.state.value
        assertEquals(GlassLinkStatus.CONNECTED, currentState.link)
        assertEquals(GlassAudioRouteStatus.RAYBAN_SPEAKER_ACTIVE, currentState.audioRoute)
        assertNotNull(currentState.device)
        assertEquals("Mock Ray-Ban Meta", currentState.device?.deviceName)
        assertEquals(90, currentState.device?.batteryLevel)
        assertTrue(currentState.isReadyForInspection)
    }

    @Test
    @DisplayName("미등록 상태에서 connect 호출 시 실패하고 unhandledError에 DeviceNotFound가 기록되어야 한다")
    fun testConnectionFailsWhenUnregistered() = runTest {
        val unregisteredState = GlassState(
            registration = GlassRegistrationStatus.UNREGISTERED,
            link = GlassLinkStatus.DISCONNECTED,
        )
        val adapter = MockGlassConnectionAdapter(initialState = unregisteredState, simulatedDelayMs = 0L)

        val result = adapter.connect()
        assertTrue(result.isFailure)

        val currentState = adapter.state.value
        assertEquals(GlassLinkStatus.DISCONNECTED, currentState.link)
        assertEquals(GlassError.DeviceNotFound, currentState.unhandledError)

        // 에러 클리어 검증
        adapter.clearError()
        assertNull(adapter.state.value.unhandledError)
    }

    @Test
    @DisplayName("disconnect 호출 시 기기 정보가 초기화되고 상태가 DISCONNECTED로 변경되어야 한다")
    fun testDisconnect() = runTest {
        val adapter = MockGlassConnectionAdapter(simulatedDelayMs = 0L)
        adapter.connect()
        assertTrue(adapter.state.value.isConnected)

        adapter.disconnect()
        val currentState = adapter.state.value
        assertEquals(GlassLinkStatus.DISCONNECTED, currentState.link)
        assertEquals(GlassAudioRouteStatus.NOT_CONNECTED, currentState.audioRoute)
        assertNull(currentState.device)
        assertFalse(currentState.isConnected)
    }

    @Test
    @DisplayName("다중 구독자(Multi-Collector)가 동시에 이벤트를 수집할 수 있어야 한다")
    fun testMultipleEventCollectors() = runTest {
        val adapter = MockGlassConnectionAdapter(simulatedDelayMs = 0L)

        val eventsCollector1 = mutableListOf<GlassConnectionEvent>()
        val eventsCollector2 = mutableListOf<GlassConnectionEvent>()

        val job1 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            adapter.events.toList(eventsCollector1)
        }
        val job2 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            adapter.events.toList(eventsCollector2)
        }

        adapter.connect()

        // 두 컬렉터 모두 Connected 이벤트를 온전히 수신했는지 검증
        assertTrue(eventsCollector1.any { it is GlassConnectionEvent.Connected })
        assertTrue(eventsCollector2.any { it is GlassConnectionEvent.Connected })

        job1.cancel()
        job2.cancel()
    }

    @Test
    @DisplayName("동시에 여러 connect/disconnect 호출이 발생해도 Mutex로 직렬화되어 안전해야 한다")
    fun testConcurrentConnectAndDisconnect() = runTest {
        val adapter = MockGlassConnectionAdapter(simulatedDelayMs = 10L)

        val connectDeferred = async { adapter.connect() }
        val disconnectDeferred = async { adapter.disconnect() }

        connectDeferred.await()
        disconnectDeferred.await()

        // 최종 상태는 마지막 실행된 disconnect 상태여야 함
        assertEquals(GlassLinkStatus.DISCONNECTED, adapter.state.value.link)
        assertNull(adapter.state.value.device)
    }

    @Test
    @DisplayName("시뮬레이터 헬퍼 함수를 통해 배터리, 스트리밍, 비정상 연결 끊김을 시뮬레이션할 수 있어야 한다")
    fun testSimulationHelpers() = runTest {
        val adapter = MockGlassConnectionAdapter(simulatedDelayMs = 0L)
        adapter.connect()

        // 1. 배터리 시뮬레이션
        adapter.simulateBattery(level = 15, isCharging = true)
        assertEquals(15, adapter.state.value.device?.batteryLevel)
        assertTrue(adapter.state.value.device?.isCharging == true)
        assertTrue(adapter.state.value.device?.isLowBattery == true)

        // 2. 스트리밍 시뮬레이션
        adapter.simulateStreaming(true)
        assertEquals(GlassStreamStatus.STREAMING, adapter.state.value.stream)
        assertTrue(adapter.state.value.isStreaming)

        // 3. 에러 시뮬레이션
        val customError = GlassError.BluetoothDisabled
        adapter.simulateError(customError)
        assertEquals(customError, adapter.state.value.unhandledError)

        // 4. 비정상 연결 끊김 시뮬레이션
        adapter.simulateDeviceDrop()
        assertEquals(GlassLinkStatus.DISCONNECTED, adapter.state.value.link)
        assertEquals(GlassError.ConnectionTimeout, adapter.state.value.unhandledError)
    }
}
