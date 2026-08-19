package com.seipseip.feature.media.data

import android.graphics.Bitmap
import com.seipseip.feature.media.domain.MediaPlanning
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max

class JpegEncoder @Inject constructor() {
    fun encodeWithinLimit(source: Bitmap): EncodedJpeg {
        var bitmap = source
        repeat(MAX_SCALE_STEPS + 1) { scaleStep ->
            JPEG_QUALITIES.forEach { quality ->
                val bytes = ByteArrayOutputStream().use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream))
                    stream.toByteArray()
                }
                if (bytes.size <= MediaPlanning.MAX_JPEG_BYTES) {
                    val encoded = EncodedJpeg(bytes, bitmap.width, bitmap.height)
                    if (bitmap !== source) bitmap.recycle()
                    return encoded
                }
            }
            if (scaleStep < MAX_SCALE_STEPS) {
                val scaled = Bitmap.createScaledBitmap(
                    bitmap,
                    max(1, (bitmap.width * SCALE_FACTOR).toInt()),
                    max(1, (bitmap.height * SCALE_FACTOR).toInt()),
                    true,
                )
                if (bitmap !== source) bitmap.recycle()
                bitmap = scaled
            }
        }
        if (bitmap !== source) bitmap.recycle()
        throw IllegalArgumentException("JPEG를 2MiB 이하로 변환하지 못했습니다.")
    }

    data class EncodedJpeg(val bytes: ByteArray, val width: Int, val height: Int)

    private companion object {
        val JPEG_QUALITIES = intArrayOf(90, 85, 80)
        const val SCALE_FACTOR = 0.85
        const val MAX_SCALE_STEPS = 6
    }
}
