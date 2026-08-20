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
}
