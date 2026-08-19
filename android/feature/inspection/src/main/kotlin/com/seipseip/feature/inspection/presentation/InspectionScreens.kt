package com.seipseip.feature.inspection.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.inspection.domain.model.Inspection
import com.seipseip.feature.inspection.domain.model.InspectionAnalysisStatus
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionListScreen(
    state: InspectionListUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStart: () -> Unit,
    onSelect: (UUID) -> Unit,
) {
    var confirmStart by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("임장 기록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Button(
                onClick = { confirmStart = true },
                enabled = !state.starting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("inspection-start"),
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text(if (state.starting) "임장 생성 중" else "새 임장 시작", Modifier.padding(start = 6.dp))
            }
            when (val content = state.content) {
                ContentState.Idle,
                ContentState.Loading,
                -> LoadingContent()
                ContentState.Empty -> EmptyInspections()
                is ContentState.Success -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(content.value, key = Inspection::id) { inspection ->
                        InspectionCard(inspection, onClick = { onSelect(inspection.id) })
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
                is ContentState.NetworkError -> ErrorContent(content.message, onRetry)
                is ContentState.ServerError -> ErrorContent(content.message, onRetry)
                is ContentState.ValidationError -> ErrorContent(content.message, onRetry)
            }
        }
    }

    if (confirmStart) {
        AlertDialog(
            onDismissRequest = { confirmStart = false },
            title = { Text("영상 촬영을 시작했나요?") },
            text = {
                Text("안경의 기본 고화질 녹화를 실제로 시작한 뒤 임장을 생성해 주세요. 앱은 아직 안경 촬영을 직접 제어하지 않습니다.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmStart = false
                    onStart()
                }) { Text("임장 시작") }
            },
            dismissButton = { TextButton(onClick = { confirmStart = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun InspectionCard(inspection: Inspection, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("inspection-card-${inspection.id}"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(inspection.status.displayName(), fontWeight = FontWeight.Bold)
            Text("분석: ${inspection.analysisStatus.displayName()}")
            Text(
                inspection.startedAt.format(DISPLAY_TIME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyInspections() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("inspection-list-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("아직 임장 기록이 없습니다.", style = MaterialTheme.typography.titleMedium)
        Text(
            "실제 영상 촬영을 시작할 때 새 임장을 생성하세요.",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionDetailScreen(
    state: InspectionDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) {
    var requestedStatus by remember { mutableStateOf<InspectionStatus?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("임장 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        when (val content = state.content) {
            ContentState.Idle,
            ContentState.Loading,
            -> LoadingContent(Modifier.padding(padding))
            ContentState.Empty -> ErrorContent("임장 정보를 찾을 수 없습니다.", onRetry, Modifier.padding(padding))
            is ContentState.NetworkError -> ErrorContent(content.message, onRetry, Modifier.padding(padding))
            is ContentState.ServerError -> ErrorContent(content.message, onRetry, Modifier.padding(padding))
            is ContentState.ValidationError -> ErrorContent(content.message, onRetry, Modifier.padding(padding))
            is ContentState.Success -> InspectionDetailContent(
                inspection = content.value,
                updating = state.updating,
                onEnd = { requestedStatus = InspectionStatus.ENDED },
                onCancel = { requestedStatus = InspectionStatus.CANCELLED },
                modifier = Modifier.padding(padding),
            )
        }
    }

    requestedStatus?.let { status ->
        AlertDialog(
            onDismissRequest = { requestedStatus = null },
            title = { Text(if (status == InspectionStatus.ENDED) "임장을 종료할까요?" else "임장을 취소할까요?") },
            text = {
                Text(
                    if (status == InspectionStatus.ENDED) {
                        "안경 녹화를 먼저 종료하고 영상이 휴대전화 갤러리에 저장됐는지 확인해 주세요. 종료한 임장은 되돌릴 수 없습니다."
                    } else {
                        "취소한 임장은 정상 임장으로 처리되지 않으며 되돌릴 수 없습니다."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    requestedStatus = null
                    if (status == InspectionStatus.ENDED) onEnd() else onCancel()
                }) { Text(if (status == InspectionStatus.ENDED) "종료" else "취소") }
            },
            dismissButton = { TextButton(onClick = { requestedStatus = null }) { Text("돌아가기") } },
        )
    }
}

@Composable
private fun InspectionDetailContent(
    inspection: Inspection,
    updating: Boolean,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(inspection.status.displayName(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        DetailRow("분석 상태", inspection.analysisStatus.displayName())
        DetailRow("촬영 시작", inspection.startedAt.format(DISPLAY_TIME))
        inspection.endedAt?.let { DetailRow("종료", it.format(DISPLAY_TIME)) }
        inspection.cancelledAt?.let { DetailRow("취소", it.format(DISPLAY_TIME)) }
        if (inspection.status == InspectionStatus.IN_PROGRESS) {
            Text(
                "원본 영상은 휴대전화 갤러리에만 보관됩니다. 서버에는 영상이 아닌 JPEG만 후속 단계에서 업로드합니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onEnd, enabled = !updating, modifier = Modifier.weight(1f)) {
                    Text(if (updating) "처리 중" else "촬영 종료")
                }
                OutlinedButton(onClick = onCancel, enabled = !updating, modifier = Modifier.weight(1f)) {
                    Text("임장 취소")
                }
            }
        } else if (inspection.status == InspectionStatus.ENDED) {
            Text("JPEG 추출·업로드 기능은 미디어 API 계약 확정 후 연결됩니다.")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text("불러오는 중", modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("다시 시도") }
    }
}

private fun InspectionStatus.displayName(): String = when (this) {
    InspectionStatus.IN_PROGRESS -> "진행 중"
    InspectionStatus.ENDED -> "종료됨"
    InspectionStatus.CANCELLED -> "취소됨"
}

private fun InspectionAnalysisStatus.displayName(): String = when (this) {
    InspectionAnalysisStatus.NOT_STARTED -> "분석 시작 전"
    InspectionAnalysisStatus.UPLOADING -> "사진 업로드 중"
    InspectionAnalysisStatus.QUEUED -> "분석 준비·대기 중"
    InspectionAnalysisStatus.ANALYZING -> "분석 중"
    InspectionAnalysisStatus.PARTIAL_COMPLETED -> "일부 분석 완료"
    InspectionAnalysisStatus.COMPLETED -> "분석 완료"
    InspectionAnalysisStatus.FAILED -> "분석 실패"
}

private val DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
