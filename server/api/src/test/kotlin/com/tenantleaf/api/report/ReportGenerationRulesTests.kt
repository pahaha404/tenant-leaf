package com.tenantleaf.api.report

import com.tenantleaf.api.media.MediaAnalysisState
import com.tenantleaf.api.media.MediaCaptureSource
import com.tenantleaf.api.media.MediaEntity
import com.tenantleaf.api.media.MediaUploadState
import com.tenantleaf.api.media.MediaZone
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
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

    @Test
    fun `공간 구간별 가장 신뢰도 높은 사진을 촬영 순서로 선택한다`() {
        val media = listOf(
            completedMedia(3_000, MediaZone.LIVING_ROOM, 0.71),
            completedMedia(6_000, MediaZone.LIVING_ROOM, 0.92),
            completedMedia(9_000, MediaZone.KITCHEN, 0.81),
            completedMedia(12_000, MediaZone.LIVING_ROOM, 0.85),
        )

        assertEquals(
            listOf(6_000L, 9_000L, 12_000L),
            selectReportRepresentativeMedia(media).map { it.sourceVideoOffsetMs },
        )
    }

    @Test
    fun `신뢰할 수 있는 공간 구역이 없으면 방문 시작 중간 마지막 사진을 선택한다`() {
        val media = (1L..6L).map { completedMedia(it * 3_000, MediaZone.UNKNOWN, null, uncertain = true) }

        assertEquals(
            listOf(3_000L, 9_000L, 18_000L),
            selectReportRepresentativeMedia(media).map { it.sourceVideoOffsetMs },
        )
    }

    private fun completedMedia(
        offsetMs: Long,
        zone: MediaZone,
        confidence: Double?,
        uncertain: Boolean = false,
    ): MediaEntity {
        val now = OffsetDateTime.parse("2026-08-25T12:00:00+09:00")
        return MediaEntity(
            id = UUID.randomUUID(),
            inspectionId = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            clientMediaId = UUID.randomUUID(),
            aiZone = zone,
            zoneConfidence = confidence,
            zoneUncertain = uncertain,
            declaredFileSize = 100_000,
            actualFileSize = 100_000,
            width = 1080,
            height = 1440,
            sourceVideoId = UUID.randomUUID(),
            sourceVideoOffsetMs = offsetMs,
            captureSource = MediaCaptureSource.META_GLASS,
            capturedAt = now,
            storageKey = "reports/$offsetMs.jpg",
            uploadStatus = MediaUploadState.UPLOADED,
            analysisStatus = MediaAnalysisState.COMPLETED,
            uploadedAt = now,
            createdAt = now,
            updatedAt = now,
        )
    }
}
