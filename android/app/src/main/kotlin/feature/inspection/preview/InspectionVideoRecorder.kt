package com.seipseip.app.feature.inspection.preview

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class InspectionVideoRecorder(private val context: Context) {
    private var outputFile: File? = null
    private var outputStream: BufferedOutputStream? = null

    @Volatile
    var isRecording = false
        private set

    @Volatile
    var isPaused = false
        private set

    private var recordStartTimeMs = 0L

    fun startRecording(): File? {
        stopRecording()
        try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.cacheDir
            val file = File(storageDir, "INSPECTION_${System.currentTimeMillis()}.mp4")
            outputFile = file
            outputStream = BufferedOutputStream(FileOutputStream(file))
            recordStartTimeMs = System.currentTimeMillis()
            isRecording = true
            isPaused = false
            Log.d(TAG, "Safe Inspection Video Recording started -> ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            return null
        }
    }

    fun pauseRecording() {
        if (isRecording && !isPaused) {
            isPaused = true
            Log.d(TAG, "Recording paused")
        }
    }

    fun resumeRecording() {
        if (isRecording && isPaused) {
            isPaused = false
            Log.d(TAG, "Recording resumed")
        }
    }

    @Synchronized
    fun writeRawVideoBytes(bytes: ByteArray) {
        if (!isRecording || isPaused || bytes.isEmpty()) return
        val stream = outputStream ?: return
        try {
            stream.write(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing video bytes: ${e.message}")
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return outputFile
        isRecording = false
        isPaused = false

        val stream = outputStream
        val file = outputFile
        outputStream = null
        outputFile = null

        try {
            if (stream != null) {
                stream.flush()
                stream.close()
                Log.d(TAG, "Video file stream closed successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing stream: ${e.message}")
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
            Log.d(TAG, "Registered inspection video to MediaStore: ${file.name} (size: ${file.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register to MediaStore: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "InspectionRecorder"
    }
}
