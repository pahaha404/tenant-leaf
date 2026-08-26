package com.seipseip.app.feature.inspection.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoicePlaybackTimeTest {
    @Test
    fun zeroMillisecondsIsDisplayedAsZeroSeconds() {
        assertEquals("0:00", formatPlaybackTime(0))
    }

    @Test
    fun millisecondsAreDisplayedAsMinutesAndSeconds() {
        assertEquals("1:05", formatPlaybackTime(65_999))
    }

    @Test
    fun negativePositionIsClampedToZero() {
        assertEquals("0:00", formatPlaybackTime(-1))
    }
}
