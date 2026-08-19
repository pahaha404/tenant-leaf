package com.seipseip.feature.media.domain

import android.net.Uri
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

data class VideoCandidate(
    val uri: Uri,
    val displayName: String,
    val createdAtMillis: Long,
    val durationMillis: Long,
) {
    val sourceVideoId: UUID = stableUuid("video:${uri}")
}

data class ExtractedJpeg(
    val file: File,
    val clientMediaId: UUID,
    val sourceVideoId: UUID,
    val sourceVideoOffsetMs: Long,
    val capturedAt: OffsetDateTime,
    val width: Int,
    val height: Int,
    val needsQualityReview: Boolean,
)

data class MediaUploadProgress(
    val completed: Int,
    val total: Int,
)

internal fun stableUuid(value: String): UUID =
    UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8))
