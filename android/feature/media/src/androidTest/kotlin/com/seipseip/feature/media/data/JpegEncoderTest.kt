package com.seipseip.feature.media.data

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.seipseip.feature.media.domain.MediaPlanning
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class JpegEncoderTest {
    @Test
    fun outputDoesNotExceedTwoMiB() {
        val bitmap = Bitmap.createBitmap(1800, 1800, Bitmap.Config.ARGB_8888)
        val row = IntArray(bitmap.width)
        repeat(bitmap.height) { y ->
            for (x in row.indices) row[x] = Random.nextInt() or (0xff shl 24)
            bitmap.setPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
        }

        val encoded = JpegEncoder().encodeWithinLimit(bitmap)

        assertTrue(encoded.bytes.size <= MediaPlanning.MAX_JPEG_BYTES)
        assertTrue(encoded.width > 0)
        assertTrue(encoded.height > 0)
        bitmap.recycle()
    }
}
