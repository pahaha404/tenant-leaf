package com.seipseip.app.integration

import com.seipseip.app.feature.report.ReportListStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportListApiRouteTest {
    @Test
    fun `완료 및 부분 완료 상태를 목록 표시 상태로 변환한다`() {
        assertEquals(ReportListStatus.COMPLETED, reportListStatus("COMPLETED"))
        assertEquals(ReportListStatus.PARTIAL, reportListStatus("PARTIAL_COMPLETED"))
    }

    @Test
    fun `분석 대기와 생성 중 상태는 모두 처리 중으로 표시한다`() {
        assertEquals(ReportListStatus.PROCESSING, reportListStatus("WAITING_FOR_ANALYSIS"))
        assertEquals(ReportListStatus.PROCESSING, reportListStatus("GENERATING"))
    }

    @Test
    fun `리포트가 없으면 없음 상태로 표시한다`() {
        assertEquals(ReportListStatus.NONE, reportListStatus(null))
    }
}
