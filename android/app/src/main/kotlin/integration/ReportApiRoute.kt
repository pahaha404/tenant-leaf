package com.seipseip.app.integration

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.seipseip.app.feature.report.EvidenceBoxUiModel
import com.seipseip.app.feature.report.ReportDetailScreen
import com.seipseip.app.feature.report.ReportDetailStatus
import com.seipseip.app.feature.report.ReportDetailUiModel
import com.seipseip.app.feature.report.ReportEvidenceUiModel
import com.seipseip.app.feature.report.ReportObservationUiModel
import com.seipseip.app.feature.report.ReportRepresentativePhotoUiModel
import com.seipseip.app.feature.report.ReportZoneUiModel
import com.seipseip.core.network.generated.api.ObservationsApi
import com.seipseip.core.network.generated.api.PropertiesApi
import com.seipseip.core.network.generated.api.ReportsApi
import com.seipseip.core.network.generated.model.Observation
import com.seipseip.core.network.generated.model.ReportDetail
import com.seipseip.core.network.generated.model.UpdateObservationStatusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val REPORT_POLL_INTERVAL_MS = 2_000L

internal data class AnalysisProgressFrame(val completed: Int, val failed: Int)

internal fun analysisProgressFrames(
    fromCompleted: Int,
    fromFailed: Int,
    toCompleted: Int,
    toFailed: Int,
): List<AnalysisProgressFrame> {
    val frames = mutableListOf<AnalysisProgressFrame>()
    var completed = fromCompleted.coerceAtMost(toCompleted)
    var failed = fromFailed.coerceAtMost(toFailed)
    while (completed < toCompleted) {
        completed++
        frames += AnalysisProgressFrame(completed, failed)
    }
    while (failed < toFailed) {
        failed++
        frames += AnalysisProgressFrame(completed, failed)
    }
    return frames
}

sealed interface ReportApiUiState {
    data object Loading : ReportApiUiState
    data class Ready(val model: ReportDetailUiModel, val propertyId: String) : ReportApiUiState
    data class Error(val message: String) : ReportApiUiState
}

@HiltViewModel
class ReportApiViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reportsApi: ReportsApi,
    private val propertiesApi: PropertiesApi,
    private val observationsApi: ObservationsApi,
) : ViewModel() {
    private val inspectionId = savedStateHandle.get<String>("inspectionId")?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    }
    private val _state = MutableStateFlow<ReportApiUiState>(ReportApiUiState.Loading)
    val state: StateFlow<ReportApiUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        val id = inspectionId ?: run {
            _state.value = ReportApiUiState.Error("올바르지 않은 임장 번호입니다.")
            return
        }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            do {
                val keepPolling = try {
                    val response = reportsApi.getInspectionReport(id)
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        val propertyAddress = runCatching {
                            propertiesApi.getProperty(body.propertyId).body()?.addressSummary
                        }.getOrNull()
                        publishProgressively(body.toUiModel(propertyAddress), body.propertyId.toString())
                        body.status.name in setOf("NOT_REQUESTED", "WAITING_FOR_ANALYSIS", "GENERATING")
                    } else {
                        _state.value = ReportApiUiState.Error("리포트를 불러오지 못했어요. (${response.code()})")
                        false
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.value = ReportApiUiState.Error("서버에서 리포트를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.")
                    false
                }
                if (keepPolling) delay(REPORT_POLL_INTERVAL_MS)
            } while (keepPolling && isActive)
        }
    }

    private suspend fun publishProgressively(next: ReportDetailUiModel, propertyId: String) {
        val previous = (_state.value as? ReportApiUiState.Ready)?.model
        val previousWasProcessing = previous?.status in setOf(
            ReportDetailStatus.WAITING_FOR_ANALYSIS,
            ReportDetailStatus.GENERATING,
        )
        val frames = if (previousWasProcessing && previous != null) {
            analysisProgressFrames(
                fromCompleted = previous.completedPhotoCount,
                fromFailed = previous.failedPhotoCount,
                toCompleted = next.completedPhotoCount,
                toFailed = next.failedPhotoCount,
            )
        } else {
            emptyList()
        }
        if (frames.isEmpty()) {
            _state.value = ReportApiUiState.Ready(next, propertyId)
            return
        }

        val frameDelayMs = (1_500L / frames.size).coerceIn(70L, 250L)
        frames.forEach { frame ->
            _state.value = ReportApiUiState.Ready(
                next.copy(
                    status = previous!!.status,
                    completedPhotoCount = frame.completed,
                    failedPhotoCount = frame.failed,
                ),
                propertyId,
            )
            delay(frameDelayMs)
        }
        _state.value = ReportApiUiState.Ready(next, propertyId)
    }

    fun markObservationViewed(observationId: String) {
        val id = runCatching { UUID.fromString(observationId) }.getOrNull() ?: return
        viewModelScope.launch {
            runCatching {
                observationsApi.updateObservationStatus(
                    id,
                    UpdateObservationStatusRequest(UpdateObservationStatusRequest.Status.VIEWED),
                )
            }.onSuccess { response ->
                if (response.isSuccessful) refresh()
            }
        }
    }
}

@Composable
fun ReportApiRoute(
    nickname: String,
    onBack: () -> Unit,
    onOpenProperty: (String) -> Unit,
    viewModel: ReportApiViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        ReportApiUiState.Loading -> ReportDetailScreen(
            nickname = nickname,
            onBack = onBack,
            onOpenProperty = {},
            uiModel = ReportDetailUiModel(
                status = ReportDetailStatus.GENERATING,
                propertyName = "리포트",
                inspectionDate = "",
            ),
        )
        is ReportApiUiState.Ready -> ReportDetailScreen(
            nickname = nickname,
            onBack = onBack,
            onOpenProperty = { onOpenProperty(current.propertyId) },
            uiModel = current.model,
            onRetry = viewModel::refresh,
            onMarkObservationViewed = viewModel::markObservationViewed,
        )
        is ReportApiUiState.Error -> ReportDetailScreen(
            nickname = nickname,
            onBack = onBack,
            onOpenProperty = {},
            uiModel = ReportDetailUiModel(
                status = ReportDetailStatus.ERROR,
                propertyName = "리포트",
                inspectionDate = "",
                errorMessage = current.message,
            ),
            onRetry = viewModel::refresh,
        )
    }
}

private fun ReportDetail.toUiModel(propertyAddress: String?): ReportDetailUiModel {
    val items = observations.map(Observation::toUiModel)
        .sortedWith(compareBy<ReportObservationUiModel> { it.evidence.sourceVideoOffsetMs }.thenBy { it.id })
    val statusName = status.name
    val detailStatus = when {
        statusName in setOf("NOT_REQUESTED", "WAITING_FOR_ANALYSIS") -> ReportDetailStatus.WAITING_FOR_ANALYSIS
        statusName == "GENERATING" -> ReportDetailStatus.GENERATING
        statusName == "PARTIAL_COMPLETED" -> ReportDetailStatus.PARTIAL
        statusName == "FAILED" -> ReportDetailStatus.ERROR
        items.isEmpty() -> ReportDetailStatus.EMPTY
        else -> ReportDetailStatus.COMPLETED
    }
    return ReportDetailUiModel(
        status = detailStatus,
        propertyName = propertyDisplayName,
        propertyAddress = propertyAddress,
        inspectionDate = inspectionEndedAt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
        completedPhotoCount = successfulMediaCount,
        totalPhotoCount = totalMediaCount,
        failedPhotoCount = failedMediaCount,
        serverReferenceScore = referenceScore,
        scoreIsProvisional = scoreIsProvisional,
        representativePhotos = representativePhotos.map { photo ->
            ReportRepresentativePhotoUiModel(
                id = photo.mediaId.toString(),
                imageUrl = photo.viewUrl.toString(),
                imageWidth = photo.image.width,
                imageHeight = photo.image.height,
                sourceVideoOffsetMs = photo.sourceVideoOffsetMs,
            )
        },
        zones = if (items.isEmpty()) emptyList() else listOf(ReportZoneUiModel("촬영 순서", items)),
        errorMessage = if (detailStatus == ReportDetailStatus.ERROR) "사진 분석 결과로 리포트를 만들지 못했어요." else null,
    )
}

private fun Observation.toUiModel(): ReportObservationUiModel {
    val primaryEvidence = evidence.firstOrNull { it.primary } ?: evidence.first()
    return ReportObservationUiModel(
        id = id.toString(),
        zone = "촬영 순서",
        label = title,
        confidencePercent = (confidence * 100).toInt().coerceIn(0, 100),
        description = description,
        reviewLabel = when (status.name) {
            "VIEWED" -> "확인 완료"
            "DISMISSED" -> "리포트 제외"
            else -> "검토 전"
        },
        reviewed = status.name == "VIEWED",
        evidence = ReportEvidenceUiModel(
            id = primaryEvidence.mediaId.toString(),
            imageUrl = primaryEvidence.viewUrl?.toString(),
            useSamplePlaceholder = false,
            imageWidth = primaryEvidence.image.width,
            imageHeight = primaryEvidence.image.height,
            sourceVideoOffsetMs = primaryEvidence.sourceVideoOffsetMs,
            boxes = primaryEvidence.detections.map { detection ->
                EvidenceBoxUiModel(
                    observationId = detection.observationId.toString(),
                    left = detection.box.left.toFloat(),
                    top = detection.box.top.toFloat(),
                    right = detection.box.right.toFloat(),
                    bottom = detection.box.bottom.toFloat(),
                    displayLabel = detection.displayLabel,
                    displayColor = detection.displayColor,
                    confidencePercent = (detection.confidence * 100).toInt().coerceIn(0, 100),
                )
            },
            pageLabel = "1 / ${evidence.size}",
        ),
    )
}
