package com.seipseip.app.feature.report

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportScoreTest {
    @Test
    fun `관찰 하나당 참고 점수에서 5점을 차감한다`() {
        assertEquals(100, reportReferenceScore(0))
        assertEquals(85, reportReferenceScore(3))
    }

    @Test
    fun `참고 점수는 0점 아래로 내려가지 않는다`() {
        assertEquals(0, reportReferenceScore(25))
    }

    @Test
    fun `음수 관찰 개수는 0건으로 취급한다`() {
        assertEquals(100, reportReferenceScore(-1))
    }
}
