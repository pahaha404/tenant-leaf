package com.seipseip.app.feature.property.location

import org.junit.Assert.assertEquals
import org.junit.Test

class AddressDraftTest {
    @Test
    fun normalizeAddress_trimsValidDirectInput() {
        assertEquals("가상시 가상구 가상로 1", normalizeAddress("  가상시 가상구 가상로 1  "))
    }

    @Test
    fun addressAfterLocationLookup_replacesBlankOrExistingInput() {
        assertEquals("가상시 새주소 1", addressAfterLocationLookup("", "  가상시 새주소 1  "))
        assertEquals("가상시 새주소 1", addressAfterLocationLookup("기존 주소", "가상시 새주소 1"))
    }

    @Test
    fun addressAfterLocationLookup_preservesExistingInputWhenLookupFails() {
        assertEquals("기존 주소", addressAfterLocationLookup("기존 주소", null))
        assertEquals("기존 주소", addressAfterLocationLookup("기존 주소", " "))
    }

    @Test
    fun addressWithDetail_appendsOnlyValidDetail() {
        assertEquals("가상시 새주소 1 101동 202호", addressWithDetail("가상시 새주소 1", "  101동 202호  "))
        assertEquals("가상시 새주소 1", addressWithDetail("가상시 새주소 1", " "))
    }

    @Test
    fun preferredLocationProvider_usesGpsForPrecisePermission() {
        assertEquals("gps", preferredLocationProvider(precise = true, gpsEnabled = true, networkEnabled = true))
        assertEquals("network", preferredLocationProvider(precise = false, gpsEnabled = true, networkEnabled = true))
        assertEquals("network", preferredLocationProvider(precise = true, gpsEnabled = false, networkEnabled = true))
    }
}
