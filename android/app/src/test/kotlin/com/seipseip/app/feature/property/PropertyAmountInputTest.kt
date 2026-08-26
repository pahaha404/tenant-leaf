package com.seipseip.app.feature.property

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertyAmountInputTest {
    @Test
    fun storedWonIsDisplayedInManwon() {
        assertEquals("5000", wonToManwonInput(50_000_000L))
        assertEquals("45", wonToManwonInput(450_000L))
        assertEquals("", wonToManwonInput(null))
    }

    @Test
    fun manwonInputIsConvertedBackToWon() {
        assertEquals("50000000", manwonInputToWon("5000"))
        assertEquals("450000", manwonInputToWon("45"))
        assertEquals("", manwonInputToWon(""))
    }
}
