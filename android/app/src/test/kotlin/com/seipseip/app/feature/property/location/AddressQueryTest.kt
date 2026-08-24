package com.seipseip.app.feature.property.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressQueryTest {
    @Test
    fun normalizeAddressQuery_trimsAndRequiresTwoCharacters() {
        assertEquals("서울역", normalizeAddressQuery("  서울역 "))
        assertNull(normalizeAddressQuery("가"))
    }
}
