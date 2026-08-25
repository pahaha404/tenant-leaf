package com.seipseip.app.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.seipseip.app.feature.report.ReportListItemUiModel
import com.seipseip.app.feature.report.ReportListScreen
import com.seipseip.app.feature.report.ReportListStatus
import com.seipseip.core.network.generated.api.PropertiesApi
import com.seipseip.core.network.generated.api.ReportsApi
import com.seipseip.core.network.generated.model.Property
import com.seipseip.core.network.generated.model.ReportSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReportListApiUiState {
    data object Loading : ReportListApiUiState
    data class Ready(val items: List<ReportListItemUiModel>) : ReportListApiUiState
    data class Error(val message: String) : ReportListApiUiState
}

@HiltViewModel
class ReportListApiViewModel @Inject constructor(
    private val propertiesApi: PropertiesApi,
    private val reportsApi: ReportsApi,
) : ViewModel() {
    private val _state = MutableStateFlow<ReportListApiUiState>(ReportListApiUiState.Loading)
    val state: StateFlow<ReportListApiUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.value = ReportListApiUiState.Loading
            _state.value = runCatching { loadItems() }.fold(
                onSuccess = { ReportListApiUiState.Ready(it) },
                onFailure = {
                    ReportListApiUiState.Error("리포트 목록을 불러오지 못했어요. 서버 연결을 확인한 뒤 다시 시도해 주세요.")
                },
            )
        }
    }

    private suspend fun loadItems(): List<ReportListItemUiModel> {
        val propertiesResponse = propertiesApi.listProperties(page = 0, size = 100)
        val properties = propertiesResponse.body()?.items
            ?.takeIf { propertiesResponse.isSuccessful }
            ?: error("Property list request failed: ${propertiesResponse.code()}")

        return properties.map { property ->
            val reportsResponse = reportsApi.listPropertyReports(property.id, page = 0, size = 1)
            val latestReport = reportsResponse.body()?.items
                ?.takeIf { reportsResponse.isSuccessful }
                ?.firstOrNull()
                ?: if (reportsResponse.isSuccessful) null else error("Report list request failed: ${reportsResponse.code()}")
            property.toReportListItem(latestReport)
        }
    }

}

@Composable
fun ReportListApiRoute(
    onOpenReport: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    viewModel: ReportListApiViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        ReportListApiUiState.Loading -> ReportListScreen(
            items = emptyList(),
            loading = true,
            errorMessage = null,
            onOpenReport = onOpenReport,
            onRetry = viewModel::refresh,
            onTabSelected = onTabSelected,
        )
        is ReportListApiUiState.Ready -> ReportListScreen(
            items = current.items,
            loading = false,
            errorMessage = null,
            onOpenReport = onOpenReport,
            onRetry = viewModel::refresh,
            onTabSelected = onTabSelected,
        )
        is ReportListApiUiState.Error -> ReportListScreen(
            items = emptyList(),
            loading = false,
            errorMessage = current.message,
            onOpenReport = onOpenReport,
            onRetry = viewModel::refresh,
            onTabSelected = onTabSelected,
        )
    }
}

internal fun Property.toReportListItem(report: ReportSummary?): ReportListItemUiModel {
    val listStatus = reportListStatus(report?.status?.name)
    val date = report?.inspectionEndedAt?.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    val processedCount = report?.let { it.successfulMediaCount + it.failedMediaCount } ?: 0
    val detail = when (listStatus) {
        ReportListStatus.NONE -> "아직 완료된 점검 리포트가 없어요"
        ReportListStatus.PROCESSING -> "$date · 사진 분석 $processedCount/${report?.totalMediaCount ?: 0}장"
        ReportListStatus.COMPLETED -> "$date · 확인 필요 관찰 ${report?.observationCount ?: 0}건"
        ReportListStatus.PARTIAL -> "$date · 일부 사진 제외 · 확인 필요 관찰 ${report?.observationCount ?: 0}건"
        ReportListStatus.FAILED -> "$date · 리포트 생성 실패"
    }
    return ReportListItemUiModel(
        propertyId = id.toString(),
        inspectionId = report?.inspectionId?.toString(),
        propertyName = name,
        address = addressSummary ?: "주소 미등록",
        detail = detail,
        status = listStatus,
    )
}

internal fun reportListStatus(statusName: String?): ReportListStatus = when (statusName) {
    "NOT_REQUESTED", "WAITING_FOR_ANALYSIS", "GENERATING" -> ReportListStatus.PROCESSING
    "COMPLETED" -> ReportListStatus.COMPLETED
    "PARTIAL_COMPLETED" -> ReportListStatus.PARTIAL
    "FAILED" -> ReportListStatus.FAILED
    else -> ReportListStatus.NONE
}
