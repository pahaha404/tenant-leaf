package com.seipseip.app.feature.inspection.voice

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.File

/**
 * 음성 원본은 서버로 보내지 않고, 매물별 최근 점검 기록만 기기 내부에 연결한다.
 */
data class PropertyVoiceRecord(
    val inspectionId: String,
    val audioPath: String,
    val savedAt: Long,
)

object VoiceRecordArchive {
    private const val preferencesName = "property_voice_records"
    private const val inspectionPropertyPrefix = "inspection_property_"
    private const val propertyRecordPrefix = "property_record_"

    /** 저장 완료 뒤 매물 상세와 음성 기록 화면이 최신 파일을 다시 읽게 하는 변경 번호다. */
    var version by mutableIntStateOf(0)
        private set

    fun linkInspectionToProperty(context: Context, inspectionId: String, propertyId: String) {
        preferences(context).edit().putString(inspectionPropertyPrefix + inspectionId, propertyId).apply()
    }

    fun saveResult(context: Context, result: VoiceRecordResult) {
        val inspectionId = result.inspectionId ?: return
        val audioPath = result.audioPath ?: return
        if (!File(audioPath).isFile) return
        val propertyId = preferences(context).getString(inspectionPropertyPrefix + inspectionId, null) ?: return
        val payload = JSONObject().apply {
            put("inspectionId", inspectionId)
            put("audioPath", audioPath)
            put("savedAt", System.currentTimeMillis())
        }
        preferences(context).edit().putString(propertyRecordPrefix + propertyId, payload.toString()).apply()
        version++
    }

    fun latestForProperty(context: Context, propertyId: String): PropertyVoiceRecord? {
        val raw = preferences(context).getString(propertyRecordPrefix + propertyId, null) ?: return null
        return runCatching {
            JSONObject(raw).let { entry ->
                PropertyVoiceRecord(
                    inspectionId = entry.getString("inspectionId"),
                    audioPath = entry.getString("audioPath"),
                    savedAt = entry.getLong("savedAt"),
                )
            }
        }.getOrNull()?.takeIf { File(it.audioPath).isFile }
    }

    /**
     * 매물 연결 기능을 넣기 전에 저장된 로컬 녹음 1개를 현재 매물에 연결한다.
     * 이미 다른 매물에 연결된 임장 녹음은 가져오지 않는다.
     */
    fun adoptLatestUnlinkedRecording(context: Context, propertyId: String): PropertyVoiceRecord? {
        if (latestForProperty(context, propertyId) != null) return null

        val assignedInspectionIds = preferences(context).all.keys
            .filter { it.startsWith(inspectionPropertyPrefix) }
            .map { it.removePrefix(inspectionPropertyPrefix) }
            .toSet()
        val voiceRoot = File(context.applicationContext.filesDir, "voice-records")
        val latestWav = voiceRoot.walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.extension == "wav" &&
                    file.parentFile?.name !in assignedInspectionIds
            }
            .maxByOrNull { it.lastModified() }
            ?: return null
        val inspectionId = latestWav.parentFile?.name ?: return null

        linkInspectionToProperty(context, inspectionId, propertyId)
        saveResult(
            context = context,
            result = VoiceRecordResult(
                inspectionId = inspectionId,
                audioPath = latestWav.absolutePath,
            ),
        )
        return latestForProperty(context, propertyId)
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}
