package com.seipseip.app.feature.inspection.preview

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Standard MediaCodec Surface-based MP4 Video Recorder adhering to Android Media & Scoped Storage Best Practices.
 */
class InspectionVideoRecorder(private val context: Context) {
    private val lock = Any()
    private var mediaMuxer: MediaMuxer? = null
    private var mediaEncoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var videoTrackIndex = -1
    private var isMuxerStarted = false
    private var outputFile: File? = null
    private var drainJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val width = 720
    private val height = 1280
    private val frameRate = 30
    private val bitRate = 3_500_000

    private val isRecordingState = AtomicBoolean(false)
    private val isPausedState = AtomicBoolean(false)

    val isRecording: Boolean get() = isRecordingState.get()
    val isPaused: Boolean get() = isPausedState.get()

    private var recordStartTimeMs = 0L
    private var lastWrittenPtsUs = -1L
    private var totalFramesWritten = 0

    fun getInputSurface(): Surface? = synchronized(lock) { inputSurface }

    fun startRecording(): File? = synchronized(lock) {
        stopRecordingInternal()
        try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.cacheDir
            val file = File(storageDir, "INSPECTION_${System.currentTimeMillis()}.mp4")
            outputFile = file

            mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            isMuxerStarted = false
            videoTrackIndex = -1
            lastWrittenPtsUs = -1L
            totalFramesWritten = 0
            recordStartTimeMs = SystemClock.elapsedRealtime()

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()
            mediaEncoder = encoder

            isRecordingState.set(true)
            isPausedState.set(false)

            drainJob = scope.launch {
                drainEncoder()
            }

            Log.d(TAG, "Standard MP4 Surface Recording started -> ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MP4 recording: ${e.message}", e)
            cleanupResources()
            return null
        }
    }

    fun pauseRecording() {
        if (isRecordingState.get() && isPausedState.compareAndSet(false, true)) {
            Log.d(TAG, "Recording paused")
        }
    }

    fun resumeRecording() {
        if (isRecordingState.get() && isPausedState.compareAndSet(true, false)) {
            Log.d(TAG, "Recording resumed")
        }
    }

    fun drawBitmapFrame(bitmap: Bitmap) {
        if (!isRecordingState.get() || isPausedState.get()) return
        val surface = synchronized(lock) { inputSurface } ?: return
        try {
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                surface.lockHardwareCanvas()
            } else {
                surface.lockCanvas(null)
            }
            if (canvas != null) {
                val destRect = Rect(0, 0, canvas.width, canvas.height)
                canvas.drawBitmap(bitmap, null, destRect, null)
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error drawing frame to encoder surface: ${e.message}")
        }
    }

    private fun drainEncoder() {
        val encoder = synchronized(lock) { mediaEncoder } ?: return
        val muxer = synchronized(lock) { mediaMuxer } ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRecordingState.get() && scope.isActive) {
            try {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        synchronized(lock) {
                            if (!isMuxerStarted) {
                                val newFormat = encoder.outputFormat
                                videoTrackIndex = muxer.addTrack(newFormat)
                                muxer.start()
                                isMuxerStarted = true
                                lastWrittenPtsUs = -1L
                                Log.d(TAG, "MediaMuxer started with track: $videoTrackIndex")
                            }
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && isMuxerStarted) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)

                                // Strictly monotonic 30fps synthetic progression (33,333us per frame)
                                val currentPts = if (lastWrittenPtsUs < 0L) 0L else lastWrittenPtsUs + 33_333L
                                lastWrittenPtsUs = currentPts
                                bufferInfo.presentationTimeUs = currentPts

                                synchronized(lock) {
                                    if (isMuxerStarted) {
                                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                                        totalFramesWritten++
                                    }
                                }
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "EOS reached in encoder drain")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRecordingState.get()) {
                    Log.e(TAG, "Error draining encoder: ${e.message}")
                }
            }
        }
    }

    suspend fun stopRecording(): File? = withContext(Dispatchers.IO) {
        stopRecordingInternal()
    }

    fun stopRecordingSync(): File? {
        return stopRecordingInternal()
    }

    private fun stopRecordingInternal(): File? = synchronized(lock) {
        if (!isRecordingState.compareAndSet(true, false)) return outputFile
        isPausedState.set(false)

        val encoder = mediaEncoder
        val muxer = mediaMuxer
        val surface = inputSurface
        val file = outputFile

        // Signal End of Stream to flush encoder
        try {
            encoder?.signalEndOfInputStream()
        } catch (e: Exception) {
            Log.w(TAG, "Could not signal EOS to encoder: ${e.message}")
        }

        // Wait briefly for drain job to finish flushing frames
        drainJob?.cancel()
        drainJob = null

        cleanupResources()

        val recordedDurationMs = (totalFramesWritten * 1000L / frameRate).coerceAtLeast(1000L)
        if (file != null && file.exists() && file.length() > 0) {
            registerToMediaStore(file, recordedDurationMs)
            return file
        }
        return null
    }

    private fun cleanupResources() {
        val surface = inputSurface
        val encoder = mediaEncoder
        val muxer = mediaMuxer

        mediaEncoder = null
        mediaMuxer = null
        inputSurface = null
        isMuxerStarted = false
        videoTrackIndex = -1

        try {
            surface?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Surface release error: ${e.message}")
        }

        try {
            encoder?.run {
                runCatching { stop() }
                runCatching { release() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Encoder release error: ${e.message}")
        }

        try {
            muxer?.run {
                if (isMuxerStarted) {
                    runCatching { stop() }
                }
                runCatching { release() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Muxer release error: ${e.message}")
        }
    }

    private fun registerToMediaStore(file: File, durationMs: Long): Uri? {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.WIDTH, width)
                put(MediaStore.Video.Media.HEIGHT, height)
                put(MediaStore.Video.Media.DURATION, durationMs)
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TenantLeaf")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input ->
                        input.copyTo(out)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }

                val latch = CountDownLatch(1)
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("video/mp4")) { _, _ ->
                    latch.countDown()
                }
                latch.await(1, TimeUnit.SECONDS)
                Log.d(TAG, "Successfully published MP4 video to MediaStore: $uri (${file.length()} bytes, duration: ${durationMs}ms)")
                return uri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish MP4 video to MediaStore: ${e.message}", e)
        }
        return null
    }

    companion object {
        private const val TAG = "InspectionRecorder"
    }
}
