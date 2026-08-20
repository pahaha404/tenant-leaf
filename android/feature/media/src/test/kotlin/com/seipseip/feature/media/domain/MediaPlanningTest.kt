package com.seipseip.feature.media.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MediaPlanningTest {
    @Test
    fun `10분 영상은 3초 구간 200개로 나눈다`() {
        val intervals = MediaPlanning.intervals(10 * 60 * 1_000L)

        assertEquals(200, intervals.size)
        assertEquals(0L..2_999L, intervals.first())
        assertEquals(597_000L..599_999L, intervals.last())
    }

    @Test
    fun `각 구간은 앞 중간 뒤 후보를 만든다`() {
        assertEquals(listOf(250L, 1_500L, 2_749L), MediaPlanning.candidateOffsets(0L..2_999L))
    }

    @Test
    fun `20장을 넘으면 여러 등록 묶음으로 나눈다`() {
        assertEquals(listOf(20, 20, 5), MediaPlanning.batches((1..45).toList()).map { it.size })
    }

    @Test
    fun `같은 영상과 시점은 같은 미디어 및 멱등성 식별자를 만든다`() {
        val source = UUID.randomUUID()

        assertEquals(MediaPlanning.clientMediaId(source, 3_000), MediaPlanning.clientMediaId(source, 3_000))
        assertEquals(
            MediaPlanning.idempotencyKey("register", source, "0"),
            MediaPlanning.idempotencyKey("register", source, "0"),
        )
        assertTrue(MediaPlanning.clientMediaId(source, 3_000) != MediaPlanning.clientMediaId(source, 6_000))
    }
}
