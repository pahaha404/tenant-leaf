package com.seipseip.app.feature.property.location

import org.junit.Assert.assertEquals
import org.junit.Test

class AddressDraftTest {
    @Test
    fun normalizeAddress_trimsValidDirectInput() {
        assertEquals("가상시 가상구 가상로 1", normalizeAddress("  가상시 가상구 가상로 1  "))
    }
}
