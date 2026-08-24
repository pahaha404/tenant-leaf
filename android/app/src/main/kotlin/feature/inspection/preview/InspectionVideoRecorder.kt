package com.seipseip.app.feature.inspection.preview

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.core.content.ContextCompat
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
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Standard MediaCodec Surface-based MP4 Video Recorder with Audio (AAC) adhering to Android Best Practices.
 */
class InspectionVideoRecorder(private val context: Context) {
    private val lock = Any()
    private var mediaMuxer: MediaMuxer? = null
    private var mediaEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var audioRecord: AudioRecord? = null
    private var inputSurface: Surface? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isMuxerStarted = false
    private var outputFile: File? = null
    private var drainJob: Job? = null
    private var audioRecordJob: Job? = null
    private var audioDrainJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val width = 720
    private val height = 1280
    private val frameRate = 30
    private val bitRate = 3_500_000

    private val audioSampleRate = 44100
    private val audioChannelCount = 1
    private val audioBitRate = 128_000

    private val isRecordingState = AtomicBoolean(false)
    private val isPausedState = AtomicBoolean(false)

    val isRecording: Boolean get() = isRecordingState.get()
    val isPaused: Boolean get() = isPausedState.get()

    private var recordStartTimeMs = 0L
    private var lastWrittenVideoPtsUs = -1L
    private var lastWrittenAudioPtsUs = -1L
    private var totalFramesWritten = 0
    private var totalPausedDurationMs = 0L
    private var lastPauseStartTimeMs = 0L

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
            audioTrackIndex = -1
            hasAudioTrack = false
            lastWrittenVideoPtsUs = -1L
            lastWrittenAudioPtsUs = -1L
            totalFramesWritten = 0
            totalPausedDurationMs = 0L
            lastPauseStartTimeMs = 0L
            finalRecordedDurationSeconds = 0L
            recordStartTimeMs = SystemClock.elapsedRealtime()

            // 1. Configure Video Encoder (H.264 / AVC)
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()
            mediaEncoder = encoder

            // 2. Configure Audio Encoder (AAC) if permission granted
            setupAudioRecording()

            isRecordingState.set(true)
            isPausedState.set(false)

            drainJob = scope.launch {
                drainVideoEncoder()
            }

            Log.d(TAG, "Standard MP4 Surface Recording started -> ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MP4 recording: ${e.message}", e)
            cleanupResources()
            return null
        }
    }

    private fun setupAudioRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted, continuing in video-only mode")
            return
        }
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                audioSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufferSize <= 0) return

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                audioSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return
            }

            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, audioSampleRate, audioChannelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2)
            }
            val aEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            aEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            aEncoder.start()
            record.startRecording()

            audioRecord = record
            audioEncoder = aEncoder
            synchronized(lock) {
                hasAudioTrack = true
            }

            audioRecordJob = scope.launch {
                pumpAudioToEncoder(record, aEncoder, minBufferSize)
            }
            audioDrainJob = scope.launch {
                drainAudioEncoder(aEncoder)
            }
            Log.d(TAG, "Audio AAC Recording pipeline started successfully")
        } catch (e: Exception) {
            synchronized(lock) {
                hasAudioTrack = false
            }
            Log.w(TAG, "Failed to initialize Audio recording pipeline: ${e.message}")
        }
    }

    private fun pumpAudioToEncoder(record: AudioRecord, encoder: MediaCodec, bufferSize: Int) {
        val audioBuffer = ByteArray(bufferSize)
        while (isRecordingState.get() && scope.isActive) {
            if (isPausedState.get()) {
                SystemClock.sleep(20)
                continue
            }
            val readBytes = record.read(audioBuffer, 0, audioBuffer.size)
            if (readBytes > 0) {
                try {
                    val inputBufferIndex = encoder.dequeueInputBuffer(10_000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            inputBuffer.put(audioBuffer, 0, readBytes)
                            val currentElapsedMs = SystemClock.elapsedRealtime() - recordStartTimeMs - totalPausedDurationMs
                            val ptsUs = (currentElapsedMs * 1000L).coerceAtLeast(0L)
                            encoder.queueInputBuffer(inputBufferIndex, 0, readBytes, ptsUs, 0)
                        }
                    }
                } catch (e: Exception) {
                    if (isRecordingState.get()) Log.w(TAG, "Error pumping audio: ${e.message}")
                }
            }
        }
    }

    fun pauseRecording() {
        if (isRecordingState.get() && isPausedState.compareAndSet(false, true)) {
            lastPauseStartTimeMs = SystemClock.elapsedRealtime()
            Log.d(TAG, "Recording paused")
        }
    }

    fun resumeRecording() {
        if (isRecordingState.get() && isPausedState.compareAndSet(true, false)) {
            if (lastPauseStartTimeMs > 0L) {
                totalPausedDurationMs += (SystemClock.elapsedRealtime() - lastPauseStartTimeMs)
                lastPauseStartTimeMs = 0L
            }
            Log.d(TAG, "Recording resumed")
        }
    }

    private var finalRecordedDurationSeconds = 0L

    fun getRecordedDurationSeconds(): Long {
        if (!isRecordingState.get()) return finalRecordedDurationSeconds
        val now = if (isPausedState.get() && lastPauseStartTimeMs > 0L) lastPauseStartTimeMs else SystemClock.elapsedRealtime()
        val elapsedMs = (now - recordStartTimeMs - totalPausedDurationMs).coerceAtLeast(0L)
        return elapsedMs / 1000L
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
                canvas.drawColor(android.graphics.Color.BLACK)
                val destRect = android.graphics.Rect(0, 0, canvas.width, canvas.height)
                val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, destRect, paint)
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error drawing frame to encoder surface: ${e.message}")
        }
    }

        private var hasAudioTrack = false

        private fun checkStartMuxerLocked() {
            val muxer = mediaMuxer ?: return
            if (isMuxerStarted) return
            val videoReady = videoTrackIndex >= 0
            val audioReady = !hasAudioTrack || audioTrackIndex >= 0
            if (videoReady && audioReady) {
                muxer.start()
                isMuxerStarted = true
                Log.d(TAG, "MediaMuxer started with videoTrack: $videoTrackIndex, audioTrack: $audioTrackIndex")
            }
        }

    private fun drainVideoEncoder() {
        val encoder = synchronized(lock) { mediaEncoder } ?: return
        val muxer = synchronized(lock) { mediaMuxer } ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRecordingState.get() && scope.isActive) {
            try {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        synchronized(lock) {
                            if (!isMuxerStarted && videoTrackIndex < 0) {
                                videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                                checkStartMuxerLocked()
                            }
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null) {
                            synchronized(lock) {
                                if (isMuxerStarted && videoTrackIndex >= 0) {
                                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                                        encodedData.position(bufferInfo.offset)
                                        encodedData.limit(bufferInfo.offset + bufferInfo.size)

                                        val currentElapsedMs = SystemClock.elapsedRealtime() - recordStartTimeMs - totalPausedDurationMs
                                        val realElapsedUs = (currentElapsedMs * 1000L).coerceAtLeast(0L)
                                        val currentPts = if (lastWrittenVideoPtsUs < 0L) {
                                            realElapsedUs
                                        } else {
                                            maxOf(lastWrittenVideoPtsUs + 1L, realElapsedUs)
                                        }
                                        lastWrittenVideoPtsUs = currentPts
                                        bufferInfo.presentationTimeUs = currentPts

                                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                                        totalFramesWritten++
                                    }
                                }
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "EOS reached in video encoder drain")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRecordingState.get()) {
                    Log.e(TAG, "Error draining video encoder: ${e.message}")
                }
            }
        }
    }

    private fun drainAudioEncoder(encoder: MediaCodec) {
        val muxer = synchronized(lock) { mediaMuxer } ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRecordingState.get() && scope.isActive) {
            try {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        synchronized(lock) {
                            if (!isMuxerStarted && audioTrackIndex < 0) {
                                audioTrackIndex = muxer.addTrack(encoder.outputFormat)
                                checkStartMuxerLocked()
                            }
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null) {
                            synchronized(lock) {
                                if (isMuxerStarted && audioTrackIndex >= 0) {
                                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                                        encodedData.position(bufferInfo.offset)
                                        encodedData.limit(bufferInfo.offset + bufferInfo.size)

                                        val currentElapsedMs = SystemClock.elapsedRealtime() - recordStartTimeMs - totalPausedDurationMs
                                        val realElapsedUs = (currentElapsedMs * 1000L).coerceAtLeast(0L)
                                        val currentPts = if (lastWrittenAudioPtsUs < 0L) {
                                            realElapsedUs
                                        } else {
                                            maxOf(lastWrittenAudioPtsUs + 1L, realElapsedUs)
                                        }
                                        lastWrittenAudioPtsUs = currentPts
                                        bufferInfo.presentationTimeUs = currentPts

                                        muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                                    }
                                }
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "EOS reached in audio encoder drain")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRecordingState.get()) {
                    Log.e(TAG, "Error draining audio encoder: ${e.message}")
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
        if (isPausedState.get() && lastPauseStartTimeMs > 0L) {
            totalPausedDurationMs += (SystemClock.elapsedRealtime() - lastPauseStartTimeMs)
            lastPauseStartTimeMs = 0L
        }
        isPausedState.set(false)

        val encoder = mediaEncoder
        val aEncoder = audioEncoder
        val aRecord = audioRecord
        val file = outputFile

        // Signal End of Stream to flush encoders
        try {
            encoder?.signalEndOfInputStream()
        } catch (e: Exception) {
            Log.w(TAG, "Could not signal EOS to video encoder: ${e.message}")
        }

        drainJob?.cancel()
        audioRecordJob?.cancel()
        audioDrainJob?.cancel()
        drainJob = null
        audioRecordJob = null
        audioDrainJob = null

        cleanupResources()

        val recordedDurationMs = if (lastWrittenVideoPtsUs > 0L) {
            (lastWrittenVideoPtsUs / 1000L).coerceAtLeast(1000L)
        } else {
            (SystemClock.elapsedRealtime() - recordStartTimeMs - totalPausedDurationMs).coerceAtLeast(1000L)
        }
        finalRecordedDurationSeconds = recordedDurationMs / 1000L
        if (file != null && file.exists() && file.length() > 0) {
            registerToMediaStore(file, recordedDurationMs)
            return file
        }
        return null
    }

    private fun cleanupResources() {
        val surface = inputSurface
        val encoder = mediaEncoder
        val aEncoder = audioEncoder
        val aRecord = audioRecord
        val muxer = mediaMuxer

        mediaEncoder = null
        audioEncoder = null
        audioRecord = null
        mediaMuxer = null
        inputSurface = null
        isMuxerStarted = false
        videoTrackIndex = -1
        audioTrackIndex = -1

        try {
            surface?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Surface release error: ${e.message}")
        }

        try {
            aRecord?.run {
                runCatching { stop() }
                runCatching { release() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord release error: ${e.message}")
        }

        try {
            aEncoder?.run {
                runCatching { stop() }
                runCatching { release() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio encoder release error: ${e.message}")
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
