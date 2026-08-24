package com.seipseip.app.feature.inspection.preview

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

data class ParsedNalUnit(
    val nalType: Int,
    val isConfig: Boolean,
    val isKeyFrame: Boolean,
    val data: ByteArray,
)

class HevcNalParser {
    private var vps: ByteArray? = null
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null

    /**
     * Splits an Annex-B byte stream into individual NAL units and converts them
     * to HVCC 4-byte length-prefixed format suitable for MediaMuxer.
     */
    fun parseAnnexBNalUnits(bytes: ByteArray): List<ParsedNalUnit> {
        if (bytes.isEmpty()) return emptyList()
        val nalUnits = mutableListOf<ParsedNalUnit>()
        val startPositions = mutableListOf<Pair<Int, Int>>() // Pair(nalStartPos, prefixLength)

        var i = 0
        while (i < bytes.size - 2) {
            if (bytes[i] == 0.toByte() && bytes[i + 1] == 0.toByte()) {
                if (bytes[i + 2] == 1.toByte()) {
                    startPositions.add(Pair(i + 3, 3))
                    i += 3
                    continue
                } else if (i < bytes.size - 3 && bytes[i + 2] == 0.toByte() && bytes[i + 3] == 1.toByte()) {
                    startPositions.add(Pair(i + 4, 4))
                    i += 4
                    continue
                }
            }
            i++
        }

        if (startPositions.isEmpty()) {
            return emptyList()
        }

        for (idx in startPositions.indices) {
            val start = startPositions[idx].first
            val end = if (idx + 1 < startPositions.size) {
                val nextStartWithPrefix = startPositions[idx + 1].first - startPositions[idx + 1].second
                nextStartWithPrefix
            } else {
                bytes.size
            }

            val length = end - start
            if (length <= 0) continue

            val nalData = ByteArray(length)
            System.arraycopy(bytes, start, nalData, 0, length)

            val nalType = (nalData[0].toInt() and 0x7E) shr 1
            val isConfig = nalType in 32..34
            val isKeyFrame = nalType in 16..21

            when (nalType) {
                32 -> vps = nalData
                33 -> sps = nalData
                34 -> pps = nalData
            }

            nalUnits.add(
                ParsedNalUnit(
                    nalType = nalType,
                    isConfig = isConfig,
                    isKeyFrame = isKeyFrame,
                    data = nalData,
                ),
            )
        }

        return nalUnits
    }

    /**
     * Builds csd-0 ByteBuffer containing VPS + SPS + PPS formatted with Annex-B start codes.
     */
    fun buildCsd0(): ByteBuffer? {
        val v = vps ?: return null
        val s = sps ?: return null
        val p = pps ?: return null

        val startCode = byteArrayOf(0, 0, 0, 1)
        val baos = ByteArrayOutputStream()
        baos.write(startCode)
        baos.write(v)
        baos.write(startCode)
        baos.write(s)
        baos.write(startCode)
        baos.write(p)

        val fullBytes = baos.toByteArray()
        return ByteBuffer.wrap(fullBytes)
    }

    /**
     * Converts raw NAL unit byte array to 4-byte big-endian length-prefixed MP4 sample data.
     */
    fun toLengthPrefixedSample(nalData: ByteArray): ByteBuffer {
        val buffer = ByteBuffer.allocate(4 + nalData.size)
        buffer.putInt(nalData.size) // 4-byte length prefix
        buffer.put(nalData)
        buffer.flip()
        return buffer
    }
}
