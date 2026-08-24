package com.seipseip.app.feature.inspection.preview

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

class InspectionVideoRecorder(private val context: Context) {
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var isMuxerStarted = false
    private var outputFile: File? = null
    private var recordStartTimeNs = 0L
    private var lastPauseTimeNs = 0L
    private var totalPausedDurationNs = 0L

    @Volatile
    var isRecording = false
        private set

    @Volatile
    var isPaused = false
        private set

    private var recorderScope = CoroutineScope(Dispatchers.IO)
    private var formatAdded = false

    fun startRecording(): File? {
        stopRecording()
        try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.cacheDir
            val file = File(storageDir, "INSPECTION_${System.currentTimeMillis()}.mp4")
            outputFile = file

            mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            isMuxerStarted = false
            formatAdded = false
            videoTrackIndex = -1
            recordStartTimeNs = System.nanoTime()
            lastPauseTimeNs = 0L
            totalPausedDurationNs = 0L
            isRecording = true
            isPaused = false
            Log.d(TAG, "Recording started -> ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
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
                totalPausedDurationNs += (System.nanoTime() - lastPauseTimeNs)
                lastPauseTimeNs = 0L
            }
            isPaused = false
            Log.d(TAG, "Recording resumed")
        }
    }

    private var hasReceivedFirstKeyFrame = false
    private var lastWrittenPtsUs = -1L

    @Synchronized
    fun onVideoFormatAvailable(format: MediaFormat) {
        val muxer = mediaMuxer ?: return
        if (!formatAdded) {
            videoTrackIndex = muxer.addTrack(format)
            muxer.start()
            isMuxerStarted = true
            formatAdded = true
            hasReceivedFirstKeyFrame = false
            lastWrittenPtsUs = -1L
            Log.d(TAG, "Muxer started with track index: $videoTrackIndex")
        }
    }

    @Synchronized
    fun writeEncodedFrame(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!isRecording || isPaused || !isMuxerStarted) return
        val muxer = mediaMuxer ?: return
        if (videoTrackIndex < 0) return

        // 1. Skip CODEC_CONFIG frames from sample data
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            return
        }

        // 2. Prevent MPEG4Writer SIGABRT crash: first frame MUST be a KEY_FRAME
        val isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
        if (!hasReceivedFirstKeyFrame) {
            if (!isKeyFrame) return
            hasReceivedFirstKeyFrame = true
            Log.d(TAG, "First key frame received for Muxer.")
        }

        try {
            val adjustedPts = (bufferInfo.presentationTimeUs - (totalPausedDurationNs / 1000L)).coerceAtLeast(0L)
            val finalPts = if (adjustedPts <= lastWrittenPtsUs) lastWrittenPtsUs + 1_000L else adjustedPts
            lastWrittenPtsUs = finalPts

            val sampleInfo = MediaCodec.BufferInfo().apply {
                set(bufferInfo.offset, bufferInfo.size, finalPts, bufferInfo.flags)
            }
            muxer.writeSampleData(videoTrackIndex, byteBuffer, sampleInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing sample data: ${e.message}")
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return outputFile
        isRecording = false
        isPaused = false

        val muxer = mediaMuxer
        val file = outputFile
        mediaMuxer = null
        outputFile = null

        try {
            if (muxer != null) {
                if (isMuxerStarted) {
                    muxer.stop()
                }
                muxer.release()
                Log.d(TAG, "Muxer stopped and released successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping muxer: ${e.message}")
        }

        if (file != null && file.exists() && file.length() > 0) {
            registerToMediaStore(file)
            return file
        }
        return null
    }

    private fun registerToMediaStore(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Video.Media.DATA, file.absolutePath)
            }
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            Log.d(TAG, "Registered inspection video to MediaStore: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register to MediaStore: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "InspectionRecorder"
    }
}
