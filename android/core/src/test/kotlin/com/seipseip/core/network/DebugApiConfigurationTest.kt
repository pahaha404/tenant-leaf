package com.seipseip.core.network

import com.seipseip.core.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugApiConfigurationTest {
    @Test
    fun `debug app connects to the shared presentation server by default`() {
        assertEquals(
            "https://stat-fashion-picture-volvo.trycloudflare.com/api/v1/",
            BuildConfig.API_BASE_URL,
        )
    }
}
