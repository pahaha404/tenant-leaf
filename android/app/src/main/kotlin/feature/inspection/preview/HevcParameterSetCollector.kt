package com.seipseip.app.feature.inspection.preview

/** Keeps VPS/SPS/PPS until a recreated Compose Surface can start a decoder. */
class HevcParameterSetCollector {
    private val sets = linkedMapOf<Int, ByteArray>()

    @Synchronized
    fun offer(frame: ByteArray) {
        if (sets.size == 3) return
        nalUnits(frame).forEach { (type, start, end) ->
            if (type in 32..34) sets[type] = frame.copyOfRange(start, end)
        }
    }

    @Synchronized
    fun complete(): ByteArray? = listOfNotNull(sets[32], sets[33], sets[34])
        .takeIf { it.size == 3 }
        ?.reduce(ByteArray::plus)

    @Synchronized
    fun clear() = sets.clear()

    private fun nalUnits(data: ByteArray): List<Triple<Int, Int, Int>> {
        val starts = mutableListOf<Pair<Int, Int>>()
        var index = 0
        while (index + 3 < data.size) {
            val length = when {
                data[index] == 0.toByte() && data[index + 1] == 0.toByte() && data[index + 2] == 1.toByte() -> 3
                data[index] == 0.toByte() && data[index + 1] == 0.toByte() && data[index + 2] == 0.toByte() && data[index + 3] == 1.toByte() -> 4
                else -> { index++; continue }
            }
            val header = index + length
            if (header < data.size && !(header + 2 < data.size && data[header] == 0.toByte() && data[header + 1] == 0.toByte() && (data[header + 2] == 1.toByte() || (header + 3 < data.size && data[header + 2] == 0.toByte() && data[header + 3] == 1.toByte())))) {
                starts += ((data[header].toInt() and 0x7e) shr 1) to index
            }
            index = header
        }
        return starts.mapIndexed { position, (type, start) -> Triple(type, start, starts.getOrNull(position + 1)?.second ?: data.size) }
    }
}
