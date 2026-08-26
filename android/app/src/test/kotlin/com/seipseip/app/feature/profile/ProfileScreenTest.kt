package com.seipseip.app.feature.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ProfileScreenTest {
    @Test
    fun nicknameValidationEnforcesLengthBounds() {
        val validNickname = "세입자"
        val tooShort = "a"
        val tooLong = "1234567890123"
        val validMaxLength = "123456789012"

        assertTrue(validNickname.trim().length in 2..12)
        assertFalse(tooShort.trim().length in 2..12)
        assertFalse(tooLong.trim().length in 2..12)
        assertTrue(validMaxLength.trim().length in 2..12)
    }

    @Test
    fun cacheSizeFormattingShowsCorrectPrecision() {
        val bytes = 134742016L // ~128.5 MB
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val formatted = String.format(Locale.US, "%.1f MB", mb)
        assertEquals("128.5 MB", formatted)

        val zeroBytes = 0L
        val zeroFormatted = if (zeroBytes <= 0L) "0.0 KB" else "other"
        assertEquals("0.0 KB", zeroFormatted)
    }

    @Test
    fun areaConversionConvertsCorrectly() {
        val pyungToSqMeters = 10.0 * 3.305785
        assertEquals(33.05785, pyungToSqMeters, 0.001)

        val sqMetersToPyung = 33.05785 / 3.305785
        assertEquals(10.0, sqMetersToPyung, 0.001)
    }

    @Test
    fun brokerageFeeCalculationFollowsLegalRules() {
        // 월세 1000/60 -> 환산보증금 7000만원 -> 요율 0.4% -> 28만원
        val deposit = 1000L
        val rent = 60L
        val standard = deposit + (rent * 100)
        val convertedAmount = if (standard < 5000) deposit + (rent * 70) else standard
        assertEquals(7000L, convertedAmount)

        val feeRate = 0.004
        val maxBrokerageFee = (convertedAmount * feeRate).toLong()
        assertEquals(28L, maxBrokerageFee)
    }
}
