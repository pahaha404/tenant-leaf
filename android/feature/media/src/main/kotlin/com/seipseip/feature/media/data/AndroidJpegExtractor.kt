package com.seipseip.feature.media.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.seipseip.feature.media.domain.ExtractedJpeg
import com.seipseip.feature.media.domain.MediaPlanning
import com.seipseip.feature.media.domain.VideoCandidate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max

class AndroidJpegExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jpegEncoder: JpegEncoder,
) {
    suspend fun extract(
        video: VideoCandidate,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): List<ExtractedJpeg> = withContext(Dispatchers.IO) {
        val intervals = MediaPlanning.intervals(video.durationMillis)
        val outputDirectory = File(context.cacheDir, "media_upload/${video.sourceVideoId}").apply { mkdirs() }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, video.uri)
            intervals.mapIndexed { index, interval ->
                coroutineContext.ensureActive()
                val selected = selectSharpest(retriever, MediaPlanning.candidateOffsets(interval))
                    ?: throw IllegalArgumentException("영상에서 JPEG 프레임을 추출하지 못했습니다.")
                try {
                    val clientMediaId = MediaPlanning.clientMediaId(video.sourceVideoId, selected.offsetMillis)
                    val output = File(outputDirectory, "${selected.offsetMillis}_$clientMediaId.jpg")
                    val encoded = jpegEncoder.encodeWithinLimit(selected.bitmap)
                    output.writeBytes(encoded.bytes)
                    onProgress(index + 1, intervals.size)
                    ExtractedJpeg(
                        file = output,
                        clientMediaId = clientMediaId,
                        sourceVideoId = video.sourceVideoId,
                        sourceVideoOffsetMs = selected.offsetMillis,
                        capturedAt = Instant.ofEpochMilli(video.createdAtMillis + selected.offsetMillis)
                            .atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                        width = encoded.width,
                        height = encoded.height,
                        needsQualityReview = selected.score < PROVISIONAL_QUALITY_REVIEW_SCORE,
                    )
                } finally {
                    selected.bitmap.recycle()
                }
            }
        } finally {
            retriever.release()
        }
    }

    private fun selectSharpest(retriever: MediaMetadataRetriever, offsets: List<Long>): FrameCandidate? {
        var best: FrameCandidate? = null
        offsets.forEach { offset ->
            val bitmap = retriever.getFrameAtTime(offset * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST) ?: return@forEach
            val candidate = FrameCandidate(bitmap, offset, edgeScore(bitmap))
            if (best == null || candidate.score > best!!.score) {
                best?.bitmap?.recycle()
                best = candidate
            } else {
                bitmap.recycle()
            }
        }
        return best
    }

    private fun edgeScore(bitmap: Bitmap): Double {
        val stepX = max(1, bitmap.width / SCORE_SAMPLE_WIDTH)
        val stepY = max(1, bitmap.height / SCORE_SAMPLE_HEIGHT)
        var total = 0.0
        var count = 0
        var y = stepY
        while (y < bitmap.height) {
            var x = stepX
            while (x < bitmap.width) {
                total += abs(luminance(bitmap.getPixel(x, y)) - luminance(bitmap.getPixel(x - stepX, y)))
                total += abs(luminance(bitmap.getPixel(x, y)) - luminance(bitmap.getPixel(x, y - stepY)))
                count += 2
                x += stepX
            }
            y += stepY
        }
        return if (count == 0) 0.0 else total / count
    }

    private fun luminance(color: Int): Double {
        val red = color shr 16 and 0xff
        val green = color shr 8 and 0xff
        val blue = color and 0xff
        return red * 0.299 + green * 0.587 + blue * 0.114
    }

    private data class FrameCandidate(val bitmap: Bitmap, val offsetMillis: Long, val score: Double)

    companion object {
        private const val SCORE_SAMPLE_WIDTH = 96
        private const val SCORE_SAMPLE_HEIGHT = 54
        private const val PROVISIONAL_QUALITY_REVIEW_SCORE = 10.0
    }
}

