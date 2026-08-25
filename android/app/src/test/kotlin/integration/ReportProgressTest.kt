package com.seipseip.app.integration

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportProgressTest {
    @Test
    fun `배치 완료 건수도 한 장씩 증가하는 프레임으로 변환한다`() {
        assertEquals(
            listOf(
                AnalysisProgressFrame(completed = 1, failed = 0),
                AnalysisProgressFrame(completed = 2, failed = 0),
                AnalysisProgressFrame(completed = 3, failed = 0),
            ),
            analysisProgressFrames(0, 0, 3, 0),
        )
    }

    @Test
    fun `성공 건수 다음에 실패 건수도 누락 없이 반영한다`() {
        assertEquals(
            listOf(
                AnalysisProgressFrame(completed = 3, failed = 0),
                AnalysisProgressFrame(completed = 3, failed = 1),
                AnalysisProgressFrame(completed = 3, failed = 2),
            ),
            analysisProgressFrames(2, 0, 3, 2),
        )
    }
}
