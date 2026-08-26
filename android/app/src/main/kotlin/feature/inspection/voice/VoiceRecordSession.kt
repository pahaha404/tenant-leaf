package com.seipseip.app.feature.inspection.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

data class VoiceRecordResult(
    val inspectionId: String? = null,
    val recording: Boolean = false,
    val transcribing: Boolean = false,
    val audioPath: String? = null,
    val transcript: String = "",
    val summary: String = "",
)

object VoiceRecordSession {
    var result by mutableStateOf(VoiceRecordResult())
        private set
    private var recorder: VoiceRecorder? = null

    fun start(context: Context, inspectionId: String) {
        if (result.recording) return
        runCatching {
            recorder = VoiceRecorder(context).also { it.start(inspectionId) }
            result = VoiceRecordResult(inspectionId = inspectionId, recording = true)
            Log.d(TAG, "Voice file opened for inspection=$inspectionId")
        }.onFailure { error ->
            recorder = null
            result = VoiceRecordResult(inspectionId = inspectionId)
            Log.w(TAG, "Could not open voice recording file", error)
        }
    }

    fun finish(context: Context) {
        val activeRecorder = recorder
        recorder = null
        val file = activeRecorder?.stop()
        if (file == null) {
            // 중간에 화면을 나가거나 기기 입력이 끊겨도 다음 점검이 이전 세션에 묶이지 않게 정리한다.
            result = VoiceRecordResult(inspectionId = result.inspectionId)
            Log.w(TAG, "Voice recording finished without PCM data")
            return
        }
        result = VoiceRecordResult(
            inspectionId = result.inspectionId,
            transcribing = true,
            audioPath = file.wavFile.absolutePath,
        )
        Log.d(TAG, "Voice WAV saved (${file.wavFile.length()} bytes)")
        VoiceRecordArchive.saveResult(context, result)
        transcribe(context, file)
    }

    /** MP4 녹화기가 이미 읽은 PCM을 같은 STT 파일에 복사한다. */
    fun appendPcm(bytes: ByteArray, size: Int) {
        recorder?.appendPcm(bytes, size)
    }

    fun discard() {
        recorder?.discard()
        recorder = null
        result = VoiceRecordResult()
    }

    fun retryTranscription(context: Context, inspectionId: String, audioPath: String) {
        val wavFile = java.io.File(audioPath)
        val pcmFile = java.io.File(wavFile.parentFile, "${wavFile.nameWithoutExtension}.pcm")
        if (!wavFile.isFile || !pcmFile.isFile) return
        result = VoiceRecordResult(
            inspectionId = inspectionId,
            transcribing = true,
            audioPath = wavFile.absolutePath,
        )
        VoiceRecordArchive.saveResult(context, result)
        transcribe(context, VoiceRecordFile(pcmFile, wavFile, 0L))
    }

    private fun transcribe(context: Context, file: VoiceRecordFile) {
        if (Build.VERSION.SDK_INT < 33 || !SpeechRecognizer.isRecognitionAvailable(context)) {
            complete(context, "")
            return
        }
        runCatching {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            // SpeechRecognizer는 파일 전체를 즉시 읽으면 버퍼가 넘칠 수 있다.
            // Pipe로 PCM을 실제 녹음 속도에 맞춰 전달해 STT 서비스가 처리할 시간을 준다.
            val (sourceDescriptor, writerDescriptor) = ParcelFileDescriptor.createPipe()
            val completed = AtomicBoolean(false)
            val handler = Handler(Looper.getMainLooper())
            lateinit var timeout: Runnable
            val finishRecognition: (String) -> Unit = { text ->
                if (completed.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeout)
                    complete(context, text, recognizer, sourceDescriptor)
                }
            }
            timeout = Runnable {
                Log.w(TAG, "Android STT timed out while reading the recorded PCM")
                finishRecognition("")
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) = finishRecognition(
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(),
                )
                override fun onError(error: Int) {
                    Log.w(TAG, "Android STT failed with code=$error")
                    finishRecognition("")
                }
                override fun onReadyForSpeech(params: Bundle) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle) = Unit
                override fun onEvent(eventType: Int, params: Bundle) = Unit
            })
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, sourceDescriptor)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, android.media.AudioFormat.ENCODING_PCM_16BIT)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, VoiceRecorder.SAMPLE_RATE)
            })
            handler.postDelayed(timeout, STT_TIMEOUT_MILLIS)
            streamPcmAtRecordingSpeed(file, writerDescriptor)
        }.onFailure {
            Log.w(TAG, "Android STT could not start", it)
            complete(context, "")
        }
    }

    private fun streamPcmAtRecordingSpeed(file: VoiceRecordFile, writerDescriptor: ParcelFileDescriptor) {
        Thread {
            runCatching {
                FileInputStream(file.pcmFile).use { input ->
                    FileOutputStream(writerDescriptor.fileDescriptor).use { output ->
                        val buffer = ByteArray(PCM_STREAM_CHUNK_BYTES)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            output.flush()
                            val durationMillis = (read * 1_000L) / (VoiceRecorder.SAMPLE_RATE * 2L)
                            Thread.sleep(durationMillis.coerceAtLeast(1L))
                        }
                    }
                }
            }.onFailure { error ->
                // 결과 수신 뒤 pipe가 닫히는 경우도 여기로 들어올 수 있어 별도 화면 오류는 만들지 않는다.
                Log.d(TAG, "PCM stream closed: ${error.javaClass.simpleName}")
            }
            runCatching { writerDescriptor.close() }
        }.apply {
            name = "voice-stt-pcm-stream"
            isDaemon = true
            start()
        }
    }

    private fun complete(context: Context, text: String, recognizer: SpeechRecognizer? = null, descriptor: ParcelFileDescriptor? = null) {
        recognizer?.destroy(); descriptor?.close()
        result = result.copy(
            transcribing = false,
            transcript = text,
            summary = text.split(Regex("(?<=[.!?])\\s+")).take(2).joinToString(" "),
        )
        VoiceRecordArchive.saveResult(context, result)
    }

    private const val TAG = "VoiceRecordSession"
    private const val PCM_STREAM_CHUNK_BYTES = 4_410 // 44.1kHz mono PCM의 약 50ms
    private const val STT_TIMEOUT_MILLIS = 45_000L
}
