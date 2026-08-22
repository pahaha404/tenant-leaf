package com.seipseip.app.feature.property

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertySwipeTest {
    @Test
    fun `삭제 스와이프는 카드 너비의 4분의 1만 왼쪽으로 이동한다`() {
        assertEquals(-250f, propertyDeleteRevealOffset(1_000), 0f)
    }
}
