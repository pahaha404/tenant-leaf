package com.seipseip.app.feature.home

import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.DeviceSessionError
import com.meta.wearable.dat.core.types.RegistrationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassConnectionStatusTest {
    @Test
    fun nextActionSeparatesRegistrationFromSessionLifecycle() {
        assertEquals(GlassConnectionAction.REGISTER, GlassConnectionAction.nextFor(RegistrationState.AVAILABLE, null))
        assertEquals(GlassConnectionAction.START_SESSION, GlassConnectionAction.nextFor(RegistrationState.REGISTERED, null))
        assertEquals(GlassConnectionAction.END_SESSION, GlassConnectionAction.nextFor(RegistrationState.REGISTERED, DeviceSessionState.STARTED))
        assertEquals(GlassConnectionAction.NONE, GlassConnectionAction.nextFor(RegistrationState.REGISTERED, DeviceSessionState.STARTING))
    }

    @Test
    fun startedSessionIsTheOnlyConnectedStatus() {
        assertEquals(GlassConnectionStatus.CONNECTED, GlassConnectionStatus.fromSessionState(DeviceSessionState.STARTED))
        assertEquals(GlassConnectionStatus.CONNECTING, GlassConnectionStatus.fromSessionState(DeviceSessionState.STARTING))
        assertEquals(GlassConnectionStatus.DISCONNECTED, GlassConnectionStatus.fromSessionState(DeviceSessionState.STOPPED))
        assertEquals(GlassConnectionStatus.PAUSED, GlassConnectionStatus.fromSessionState(DeviceSessionState.PAUSED))
    }

    @Test
    fun sessionUnavailableReconnectsWithBoundedBackoff() {
        assertEquals(1_000L, GlassSessionReconnect.delayForAttempt(0))
        assertEquals(3_000L, GlassSessionReconnect.delayForAttempt(1))
        assertEquals(6_000L, GlassSessionReconnect.delayForAttempt(2))
        assertEquals(null, GlassSessionReconnect.delayForAttempt(3))
    }

    @Test
    fun terminalDeviceSafetyErrorsDoNotReconnect() {
        assertTrue(shouldReconnectAfter(DeviceSessionError.DEVICE_DISCONNECTED))
        assertFalse(shouldReconnectAfter(DeviceSessionError.BATTERY_CRITICAL))
        assertFalse(shouldReconnectAfter(DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED))
    }
}
