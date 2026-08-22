package com.seipseip.core.network

import com.seipseip.core.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugApiConfigurationTest {
    @Test
    fun `debug app connects to the physical device loopback address`() {
        assertEquals("http://127.0.0.1:8080/api/v1/", BuildConfig.API_BASE_URL)
    }
}
