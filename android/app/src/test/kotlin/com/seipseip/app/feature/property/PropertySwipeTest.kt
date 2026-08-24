package com.seipseip.app.feature.property

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertySwipeTest {
    @Test
    fun `삭제 스와이프는 카드 너비의 4분의 1만 왼쪽으로 이동한다`() {
        assertEquals(-250f, propertyDeleteRevealOffset(1_000), 0f)
    }

    @Test
    fun `한국 부동산 단위 금액 포맷팅 검증`() {
        assertEquals("10억", formatWon(1_000_000_000L))
        assertEquals("5.5억", formatWon(550_000_000L))
        assertEquals("1.2억", formatWon(120_000_000L))
        assertEquals("1억", formatWon(100_000_000L))
        assertEquals("5,000만", formatWon(50_000_000L))
        assertEquals("500만", formatWon(5_000_000L))
        assertEquals("45만", formatWon(450_000L))
        assertEquals("7만", formatWon(70_000L))
        assertEquals("5,000원", formatWon(5_000L))
        assertEquals("0원", formatWon(0L))
    }
}
