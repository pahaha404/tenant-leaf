package com.seipseip.app.feature.inspection.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class VoiceRecordResult(
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
            result = VoiceRecordResult(recording = true)
        }
    }

    fun finish(context: Context) {
        val file = recorder?.stop() ?: return
        recorder = null
        result = VoiceRecordResult(transcribing = true, audioPath = file.wavFile.absolutePath)
        transcribe(context, file)
    }

    fun discard() {
        recorder?.discard()
        recorder = null
        result = VoiceRecordResult()
    }

    private fun transcribe(context: Context, file: VoiceRecordFile) {
        if (Build.VERSION.SDK_INT < 33 || !SpeechRecognizer.isRecognitionAvailable(context)) return complete("")
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val descriptor = ParcelFileDescriptor.open(file.pcmFile, ParcelFileDescriptor.MODE_READ_ONLY)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) = complete(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(), recognizer, descriptor)
            override fun onError(error: Int) = complete("", recognizer, descriptor)
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
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, descriptor)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, VoiceRecorder.ENCODING)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, VoiceRecorder.SAMPLE_RATE)
        })
    }

    private fun complete(text: String, recognizer: SpeechRecognizer? = null, descriptor: ParcelFileDescriptor? = null) {
        recognizer?.destroy(); descriptor?.close()
        result = result.copy(
            transcribing = false,
            transcript = text,
            summary = text.split(Regex("(?<=[.!?])\\s+")).take(2).joinToString(" "),
        )
    }
}
