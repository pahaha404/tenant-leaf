package com.seipseip.app.integration

import com.seipseip.app.feature.report.ReportListStatus
import com.seipseip.app.feature.report.ReportListItemUiModel
import java.time.OffsetDateTime
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

    @Test
    fun `점검 종료 시각이 가장 최근인 리포트를 선택한다`() {
        val older = ReportListItemUiModel("p1", "i1", "이전 매물", "", "", ReportListStatus.COMPLETED, inspectionEndedAt = OffsetDateTime.parse("2026-08-19T10:00:00+09:00"))
        val latest = ReportListItemUiModel("p2", "i2", "최근 매물", "", "", ReportListStatus.COMPLETED, inspectionEndedAt = OffsetDateTime.parse("2026-08-24T10:00:00+09:00"))

        assertEquals("i2", latestReportItem(listOf(older, latest))?.inspectionId)
    }
}
