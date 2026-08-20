package com.seipseip.feature.property.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AreaConverterTest {
    @Test
    fun `square meters and pyeong convert in both directions without display rounding`() {
        val squareMeters = 33.05785

        val pyeong = AreaConverter.squareMetersToPyeong(squareMeters)
        val convertedBack = AreaConverter.pyeongToSquareMeters(pyeong)

        assertEquals(10.0, pyeong, 0.0000001)
        assertEquals(squareMeters, convertedBack, 0.0000001)
    }

    @Test
    fun `display rounding uses the requested decimal places`() {
        assertEquals(9.99, AreaConverter.roundForDisplay(9.994, decimalPlaces = 2), 0.0)
        assertEquals(10.0, AreaConverter.roundForDisplay(9.996, decimalPlaces = 2), 0.0)
    }
}

