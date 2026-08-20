package com.seipseip.feature.media.domain

import java.util.UUID

object MediaPlanning {
    const val INTERVAL_MS = 3_000L
    const val MAX_JPEG_BYTES = 2 * 1024 * 1024
    const val MAX_BATCH_SIZE = 20
    const val MAX_UPLOAD_ATTEMPTS = 3

    fun intervals(durationMillis: Long): List<LongRange> {
        if (durationMillis <= 0) return emptyList()
        return buildList {
            var start = 0L
            while (start < durationMillis) {
                add(start..minOf(durationMillis - 1, start + INTERVAL_MS - 1))
                start += INTERVAL_MS
            }
        }
    }

    fun candidateOffsets(interval: LongRange): List<Long> {
        val length = interval.last - interval.first + 1
        if (length <= 2) return listOf(interval.first)
        val inset = minOf(250L, length / 4)
        return listOf(
            interval.first + inset,
            interval.first + length / 2,
            interval.last - inset,
        ).distinct().filter { it in interval }
    }

    fun <T> batches(items: List<T>): List<List<T>> = items.chunked(MAX_BATCH_SIZE)

    fun clientMediaId(sourceVideoId: UUID, offsetMillis: Long): UUID =
        stableUuid("$sourceVideoId:$offsetMillis")

    fun idempotencyKey(operation: String, resourceId: UUID, discriminator: String): UUID =
        stableUuid("$operation:$resourceId:$discriminator")
}

