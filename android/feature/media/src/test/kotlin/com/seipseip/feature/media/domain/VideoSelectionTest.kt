package com.seipseip.feature.media.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VideoSelectionTest {
    private data class Candidate(val name: String, val createdAt: Long)

    @Test
    fun `후보가 없으면 선택하지 않는다`() {
        assertSame(VideoSelection.None, VideoSelection.from(emptyList<Candidate>(), Candidate::createdAt))
    }

    @Test
    fun `후보가 하나면 자동 선택한다`() {
        val only = Candidate("only", 100)
        assertEquals(only, (VideoSelection.from(listOf(only), Candidate::createdAt) as VideoSelection.Automatic).value)
    }

    @Test
    fun `후보가 여러 개면 최신순으로 확인을 요청한다`() {
        val old = Candidate("old", 100)
        val newest = Candidate("new", 300)
        val middle = Candidate("middle", 200)

        val result = VideoSelection.from(listOf(old, newest, middle), Candidate::createdAt) as VideoSelection.ConfirmationRequired

        assertEquals(listOf(newest, middle, old), result.newestFirst)
    }
}

