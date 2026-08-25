package com.seipseip.app.feature.inspection.voice

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

data class VoiceRecordFile(val pcmFile: File, val wavFile: File, val durationMillis: Long)

/**
 * MP4 녹화기가 이미 읽은 PCM을 STT 입력용 WAV로 저장한다.
 * 이 클래스는 AudioRecord를 만들지 않으므로 영상 AAC 오디오와 마이크를 경쟁하지 않는다.
 */
class VoiceRecorder(context: Context) {
    private val appContext = context.applicationContext
    private var output: BufferedOutputStream? = null
    private var pcmFile: File? = null
    private var startedAt = 0L
    @Volatile private var recording = false

    @Synchronized
    fun start(inspectionId: String) {
        if (recording) return
        val directory = File(appContext.filesDir, "voice-records/$inspectionId").apply { mkdirs() }
        val pcm = File(directory, "recording-${System.currentTimeMillis()}.pcm")
        pcmFile = pcm
        output = BufferedOutputStream(FileOutputStream(pcm))
        startedAt = System.currentTimeMillis()
        recording = true
    }

    @Synchronized
    fun appendPcm(bytes: ByteArray, size: Int) {
        if (!recording || size <= 0) return
        output?.write(bytes, 0, size)
    }

    @Synchronized
    fun stop(): VoiceRecordFile? {
        if (!recording) return null
        recording = false
        runCatching { output?.close() }
        output = null

        val pcm = pcmFile ?: return null
        pcmFile = null
        if (pcm.length() == 0L) { pcm.delete(); return null }
        val wav = File(pcm.parentFile, "${pcm.nameWithoutExtension}.wav")
        writeWav(pcm, wav)
        return VoiceRecordFile(pcm, wav, System.currentTimeMillis() - startedAt)
    }

    fun discard() {
        val file = stop()
        file?.pcmFile?.delete()
        file?.wavFile?.delete()
    }

    fun discardCurrentRecording() = discard()

    fun recordedMemoCount(inspectionKey: Long): Int =
        File(appContext.filesDir, "voice-records/$inspectionKey").listFiles { file -> file.extension == "wav" }?.size ?: 0

    private fun writeWav(pcm: File, wav: File) {
        val dataLength = pcm.length().toInt()
        RandomAccessFile(wav, "rw").use { out ->
            out.setLength(0)
            out.writeBytes("RIFF")
            out.writeInt(Integer.reverseBytes(36 + dataLength))
            out.writeBytes("WAVEfmt ")
            out.writeInt(Integer.reverseBytes(16))
            out.writeShort(java.lang.Short.reverseBytes(1).toInt())
            out.writeShort(java.lang.Short.reverseBytes(1).toInt())
            out.writeInt(Integer.reverseBytes(SAMPLE_RATE))
            out.writeInt(Integer.reverseBytes(SAMPLE_RATE * 2))
            out.writeShort(java.lang.Short.reverseBytes(2).toInt())
            out.writeShort(java.lang.Short.reverseBytes(16).toInt())
            out.writeBytes("data")
            out.writeInt(Integer.reverseBytes(dataLength))
            pcm.inputStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val bytes = input.read(buffer)
                    if (bytes < 0) break
                    out.write(buffer, 0, bytes)
                }
            }
        }
    }

    companion object {
        const val SAMPLE_RATE = 44_100
    }
}
