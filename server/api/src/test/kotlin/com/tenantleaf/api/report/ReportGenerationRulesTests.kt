package com.tenantleaf.api.report

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportGenerationRulesTests {
    @Test
    fun `AI classId 열세 가지가 관찰 유형과 사용자 문구로 매핑된다`() {
        val mappings = (0..12).map(ObservationCatalog::forClassId)

        assertEquals(13, mappings.filterNotNull().size)
        assertEquals("crack", mappings.first()?.label)
        assertEquals("other", mappings.last()?.label)
        assertEquals("하자 의심", mappings.last()?.displayLabel)
        assertNull(ObservationCatalog.forClassId(13))
    }

    @Test
    fun `참고 점수는 관찰당 5점 차감하고 0점 아래로 내려가지 않는다`() {
        assertEquals(100, calculateReferenceScore(0))
        assertEquals(85, calculateReferenceScore(3))
        assertEquals(0, calculateReferenceScore(20))
        assertEquals(0, calculateReferenceScore(30))
    }
}
