package com.seipseip.app.feature.inspection.voice

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class VoiceRecordResult(
    val inspectionId: String? = null,
    val recording: Boolean = false,
    val audioPath: String? = null,
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
            audioPath = file.wavFile.absolutePath,
        )
        Log.d(TAG, "Voice WAV saved (${file.wavFile.length()} bytes)")
        VoiceRecordArchive.saveResult(context, result)
    }

    /** MP4 녹화기가 이미 읽은 PCM을 로컬 음성 기록 파일에도 복사한다. */
    fun appendPcm(bytes: ByteArray, size: Int) {
        recorder?.appendPcm(bytes, size)
    }

    fun discard() {
        recorder?.discard()
        recorder = null
        result = VoiceRecordResult()
    }

    private const val TAG = "VoiceRecordSession"
}
