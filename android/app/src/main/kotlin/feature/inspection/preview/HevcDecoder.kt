package com.seipseip.app.feature.inspection.preview

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import android.view.Surface
import java.util.concurrent.ArrayBlockingQueue

/** DAT CameraAccess sample decoder, kept on its own thread so Compose only owns the Surface. */
class HevcDecoder {
    private data class Frame(
        val bytes: ByteArray,
        val offset: Int,
        val timestampUs: Long,
        val keyFrame: Boolean,
        val config: Boolean,
    ) {
        val flags: Int get() =
            (if (keyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0) or
                (if (config) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else 0)
    }

    companion object {
        private const val TAG = "TenantLeafHevc"
        private val blockedDecoders = setOf("OMX.Exynos.hevc.dec", "c2.mtk.hevc.decoder")
    }

    @Volatile private var queue = ArrayBlockingQueue<Frame>(100)
    @Volatile private var codec: MediaCodec? = null
    @Volatile private var decoderThread: HandlerThread? = null
    @Volatile private var format: MediaFormat? = null
    @Volatile private var surface: Surface? = null
    @Volatile private var cachedConfig: ByteArray? = null
    @Volatile private var active = false
    @Volatile private var receivedKeyFrame = false
    @Volatile private var firstInput = true
    @Volatile private var recorder: InspectionVideoRecorder? = null

    fun setRecorder(inspectionRecorder: InspectionVideoRecorder?) {
        recorder = inspectionRecorder
    }

    fun start(width: Int, height: Int, outputSurface: Surface) {
        surface = outputSurface
        format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height).apply {
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000)
        }
        runCatching { ensureCodec() }.onFailure { Log.e(TAG, "Decoder creation failed", it) }
    }

    fun decodeFrame(bytes: ByteArray, timestampUs: Long) {
        if (bytes.isEmpty()) return
        var index = findNalUnit(bytes, 0, bytes.size, BooleanArray(3))
        val prefixFlags = BooleanArray(3)
        while (index < bytes.size) {
            val type = nalType(bytes, index)
            val config = type in 32..34
            val keyFrame = type in 16..21
            if (config) cachedConfig = bytes
            if (keyFrame && !active) {
                active = true
                cachedConfig?.let(::enqueueConfig)
            }
            enqueue(Frame(bytes, index, timestampUs, keyFrame, config))
            index = findNalUnit(bytes, index + 1, bytes.size, prefixFlags)
        }
    }

    fun stop() {
        active = false
        queue.clear()
        runCatching { codec?.stop(); codec?.release() }.onFailure { Log.e(TAG, "Decoder stop failed", it) }
        codec = null
        decoderThread?.quit()
        decoderThread = null
        cachedConfig = null
        surface = null
        receivedKeyFrame = false
        firstInput = true
    }

    private fun enqueueConfig(bytes: ByteArray) {
        var index = findNalUnit(bytes, 0, bytes.size, BooleanArray(3))
        val prefixFlags = BooleanArray(3)
        while (index < bytes.size) {
            val type = nalType(bytes, index)
            enqueue(Frame(bytes, index, 0, type in 16..21, type in 32..34))
            index = findNalUnit(bytes, index + 1, bytes.size, prefixFlags)
        }
    }

    private fun enqueue(frame: Frame) {
        if (!active) return
        if (!frame.config && !receivedKeyFrame) {
            if (!frame.keyFrame) return
            receivedKeyFrame = true
        }
        if (firstInput) {
            firstInput = false
            activateCodec()
        }

        val rec = recorder
        val fmt = format
        if (rec != null && rec.isRecording && !rec.isPaused && fmt != null) {
            rec.onVideoFormatAvailable(fmt)
            val length = minOf(frame.bytes.size - frame.offset, frame.bytes.size)
            if (length > 0) {
                val bufferInfo = MediaCodec.BufferInfo().apply {
                    set(frame.offset, length, frame.timestampUs, frame.flags)
                }
                val byteBuffer = java.nio.ByteBuffer.wrap(frame.bytes, frame.offset, length)
                rec.writeEncodedFrame(byteBuffer, bufferInfo)
            }
        }

        if (!queue.offer(frame)) {
            Log.w(TAG, "Decoder queue full")
            active = false
        }
    }

    private fun ensureCodec() {
        if (codec != null) return
        val mime = MediaFormat.MIMETYPE_VIDEO_HEVC
        val software = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.firstOrNull {
            !it.isEncoder && it.isSoftwareOnly && it.name !in blockedDecoders &&
                it.supportedTypes.any { type -> type.equals(mime, ignoreCase = true) }
        }?.name
        codec = if (software == null) MediaCodec.createDecoderByType(mime) else MediaCodec.createByCodecName(software)
    }

    private fun activateCodec() {
        runCatching {
            ensureCodec()
            val currentCodec = codec ?: return@runCatching
            decoderThread?.quit()
            val thread = HandlerThread("TenantLeafHevc", Process.THREAD_PRIORITY_VIDEO).also { it.start() }
            decoderThread = thread
            currentCodec.reset()
            currentCodec.configure(format, surface, null, 0)
            currentCodec.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    var queued = false
                    try {
                        val frame = queue.poll()
                        val input = codec.getInputBuffer(index)
                        if (frame == null || input == null || !active) {
                            codec.queueInputBuffer(index, 0, 0, 0, 0)
                        } else {
                            input.clear()
                            input.put(frame.bytes)
                            input.flip()
                            codec.queueInputBuffer(index, frame.offset, minOf(frame.bytes.size - frame.offset, input.limit() - frame.offset), frame.timestampUs, frame.flags)
                        }
                        queued = true
                    } catch (error: Throwable) {
                        Log.e(TAG, "Decoder input failed", error)
                        active = false
                    } finally {
                        if (!queued) runCatching { codec.queueInputBuffer(index, 0, 0, 0, 0) }
                    }
                }

                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    runCatching { codec.releaseOutputBuffer(index, active && info.size > 0) }
                        .onFailure { Log.e(TAG, "Decoder output failed", it) }
                }
                override fun onError(codec: MediaCodec, error: MediaCodec.CodecException) { Log.e(TAG, "Decoder error", error) }
                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) = Unit
            }, Handler(thread.looper))
            currentCodec.start()
        }.onFailure { Log.e(TAG, "Decoder activation failed", it) }
    }

    private fun findNalUnit(data: ByteArray, start: Int, end: Int, flags: BooleanArray): Int {
        val length = end - start
        if (length == 0) return end
        if (flags[0]) { clearFlags(flags); return start - 3 }
        if (length > 1 && flags[1] && data[start].toInt() == 1) { clearFlags(flags); return start - 2 }
        if (length > 2 && flags[2] && data[start].toInt() == 0 && data[start + 1].toInt() == 1) { clearFlags(flags); return start - 1 }
        var index = start + 2
        while (index < end - 1) {
            if ((data[index].toInt() and 0xfe) == 0 && data[index - 2].toInt() == 0 && data[index - 1].toInt() == 0 && data[index].toInt() == 1) {
                clearFlags(flags)
                return index - 2
            }
            index += if ((data[index].toInt() and 0xfe) == 0) 1 else 3
        }
        flags[0] = length > 2 && data[end - 3].toInt() == 0 && data[end - 2].toInt() == 0 && data[end - 1].toInt() == 1
        flags[1] = if (length > 1) data[end - 2].toInt() == 0 && data[end - 1].toInt() == 0 else flags[2] && data[end - 1].toInt() == 0
        flags[2] = data[end - 1].toInt() == 0
        return end
    }

    private fun clearFlags(flags: BooleanArray) { flags[0] = false; flags[1] = false; flags[2] = false }
    private fun nalType(data: ByteArray, offset: Int): Int = if (offset + 3 >= data.size) -1 else (data[offset + 3].toInt() and 0x7e) shr 1
}
