package com.seipseip.app.feature.inspection.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.concurrent.thread

data class VoiceRecordFile(val pcmFile: File, val wavFile: File, val durationMillis: Long)

/** 임장 전체 음성을 앱 내부에 저장한다. STT 입력용 PCM과 재생용 WAV를 함께 만든다. */
class VoiceRecorder(context: Context) {
    private val appContext = context.applicationContext
    private var audioRecord: AudioRecord? = null
    private var writer: Thread? = null
    private var pcmFile: File? = null
    private var startedAt = 0L
    @Volatile private var recording = false

    fun start(inspectionId: String) {
        if (recording) return
        val directory = File(appContext.filesDir, "voice-records/$inspectionId").apply { mkdirs() }
        val pcm = File(directory, "recording-${System.currentTimeMillis()}.pcm")
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING).coerceAtLeast(4096)
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, ENCODING, bufferSize)
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "마이크를 초기화하지 못했습니다." }

        pcmFile = pcm
        audioRecord = recorder
        startedAt = System.currentTimeMillis()
        recording = true
        recorder.startRecording()
        writer = thread(name = "tenant-leaf-voice-record", start = true) {
            BufferedOutputStream(FileOutputStream(pcm)).use { output ->
                val buffer = ByteArray(bufferSize)
                while (recording) {
                    val size = recorder.read(buffer, 0, buffer.size)
                    if (size > 0) output.write(buffer, 0, size)
                }
            }
        }
    }

    fun stop(): VoiceRecordFile? {
        if (!recording) return null
        recording = false
        val recorder = audioRecord
        audioRecord = null
        runCatching { recorder?.stop() }
        recorder?.release()
        writer?.join(1_500)
        writer = null

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
        const val SAMPLE_RATE = 16_000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
