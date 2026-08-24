package com.seipseip.app.feature.inspection.preview

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

class InspectionVideoRecorder(private val context: Context) {
    private var mediaMuxer: MediaMuxer? = null
    private var mediaEncoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var videoTrackIndex = -1
    private var isMuxerStarted = false
    private var outputFile: File? = null
    private var drainJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val width = 720
    private val height = 1280
    private val frameRate = 30
    private val bitRate = 3_500_000

    @Volatile
    var isRecording = false
        private set

    @Volatile
    var isPaused = false
        private set

    private var lastPauseTimeNs = 0L
    private var totalPausedDurationUs = 0L

    fun getInputSurface(): Surface? = inputSurface

    fun startRecording(): File? {
        stopRecordingInternal()
        try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.cacheDir
            val file = File(storageDir, "INSPECTION_${System.currentTimeMillis()}.mp4")
            outputFile = file

            mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            isMuxerStarted = false
            videoTrackIndex = -1
            totalPausedDurationUs = 0L
            lastPauseTimeNs = 0L

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

            isRecording = true
            isPaused = false

            drainJob = scope.launch {
                drainEncoder()
            }

            Log.d(TAG, "Standard MP4 Surface Recording started -> ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MP4 recording: ${e.message}", e)
            return null
        }
    }

    fun pauseRecording() {
        if (isRecording && !isPaused) {
            isPaused = true
            lastPauseTimeNs = System.nanoTime()
            Log.d(TAG, "Recording paused")
        }
    }

    fun resumeRecording() {
        if (isRecording && isPaused) {
            if (lastPauseTimeNs > 0L) {
                totalPausedDurationUs += (System.nanoTime() - lastPauseTimeNs) / 1000L
                lastPauseTimeNs = 0L
            }
            isPaused = false
            Log.d(TAG, "Recording resumed")
        }
    }

    fun drawBitmapFrame(bitmap: Bitmap) {
        if (!isRecording || isPaused) return
        val surface = inputSurface ?: return
        try {
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                surface.lockHardwareCanvas()
            } else {
                surface.lockCanvas(null)
            }
            if (canvas != null) {
                val destRect = android.graphics.Rect(0, 0, canvas.width, canvas.height)
                canvas.drawBitmap(bitmap, null, destRect, null)
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error drawing frame to encoder surface: ${e.message}")
        }
    }

    private fun drainEncoder() {
        val encoder = mediaEncoder ?: return
        val muxer = mediaMuxer ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRecording && scope.isActive) {
            try {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!isMuxerStarted) {
                            val newFormat = encoder.outputFormat
                            videoTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            isMuxerStarted = true
                            Log.d(TAG, "MediaMuxer started with track: $videoTrackIndex")
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && isMuxerStarted && !isPaused) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)

                                val adjustedPts = (bufferInfo.presentationTimeUs - totalPausedDurationUs).coerceAtLeast(0L)
                                bufferInfo.presentationTimeUs = adjustedPts

                                muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
            } catch (e: Exception) {
                if (isRecording) {
                    Log.e(TAG, "Error draining encoder: ${e.message}")
                }
            }
        }
    }

    fun stopRecording(): File? {
        return stopRecordingInternal()
    }

    private fun stopRecordingInternal(): File? {
        if (!isRecording) return outputFile
        isRecording = false
        isPaused = false

        drainJob?.cancel()
        drainJob = null

        val encoder = mediaEncoder
        val muxer = mediaMuxer
        val surface = inputSurface
        val file = outputFile

        mediaEncoder = null
        mediaMuxer = null
        inputSurface = null
        outputFile = null

        try {
            surface?.release()
            if (encoder != null) {
                runCatching { encoder.stop() }
                runCatching { encoder.release() }
            }
            if (muxer != null) {
                if (isMuxerStarted) {
                    runCatching { muxer.stop() }
                }
                runCatching { muxer.release() }
            }
            Log.d(TAG, "MP4 Encoder and Muxer stopped and released successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing encoder resources: ${e.message}")
        }

        if (file != null && file.exists() && file.length() > 0) {
            registerToMediaStore(file)
            return file
        }
        return null
    }

    private fun registerToMediaStore(file: File): Uri? {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
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

                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("video/mp4"), null)
                Log.d(TAG, "Successfully copied MP4 video to MediaStore: $uri (${file.length()} bytes)")
                return uri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy MP4 video to MediaStore: ${e.message}", e)
        }
        return null
    }

    companion object {
        private const val TAG = "InspectionRecorder"
    }
}
