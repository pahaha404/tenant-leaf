package com.seipseip.app.feature.report

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.Border
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.PaleOrange
import com.seipseip.app.R
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.PrimaryButton
import com.seipseip.app.feature.common.SecondaryButton
import com.seipseip.app.feature.common.StateBadge
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ReportBackground = Color(0xFFFCFBF8)
private val SuccessGreen = Color(0xFF28B264)
private val MutedGreen = Color(0xFF73877B)
private val DarkViewer = Color(0xFF0C2B1D)
private val ErrorRed = Color(0xFFD9483B)

enum class ReportDetailStatus { WAITING_FOR_ANALYSIS, GENERATING, COMPLETED, EMPTY, PARTIAL, ERROR }

data class EvidenceBoxUiModel(
    val observationId: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val displayLabel: String,
    val displayColor: String,
    val confidencePercent: Int,
)

data class ReportEvidenceUiModel(
    val id: String,
    val imageUrl: String? = null,
    @DrawableRes val placeholderRes: Int = R.drawable.guide_bath_mold,
    val useSamplePlaceholder: Boolean = false,
    val imageWidth: Int = 1080,
    val imageHeight: Int = 1440,
    val boxes: List<EvidenceBoxUiModel> = emptyList(),
    val pageLabel: String = "1 / 1",
)

data class ReportObservationUiModel(
    val id: String,
    val zone: String,
    val label: String,
    val confidencePercent: Int,
    val description: String,
    val evidence: ReportEvidenceUiModel,
    val reviewLabel: String = "검토 전",
    val reviewed: Boolean = false,
)

data class ReportZoneUiModel(val name: String, val observations: List<ReportObservationUiModel>)

data class ReportDetailUiModel(
    val status: ReportDetailStatus,
    val propertyName: String,
    val propertyAddress: String? = null,
    val inspectionDate: String,
    val visitLabel: String = "첫 방문 점검",
    val completedPhotoCount: Int = 0,
    val totalPhotoCount: Int = 0,
    val failedPhotoCount: Int = 0,
    val serverReferenceScore: Int? = null,
    val scoreIsProvisional: Boolean = false,
    val zones: List<ReportZoneUiModel> = emptyList(),
    val errorMessage: String? = null,
) {
    val observations: List<ReportObservationUiModel> get() = zones.flatMap(ReportZoneUiModel::observations)
    val processedPhotoCount: Int get() = completedPhotoCount + failedPhotoCount
    val referenceScore: Int get() = serverReferenceScore ?: reportReferenceScore(observations.size)
}

enum class ReportListStatus { NONE, PROCESSING, COMPLETED, PARTIAL, FAILED }

data class ReportListItemUiModel(
    val propertyId: String,
    val inspectionId: String?,
    val propertyName: String,
    val address: String,
    val detail: String,
    val status: ReportListStatus,
    val dateLabel: String = "",
    val referenceScore: Int? = null,
)

fun reportReferenceScore(observationCount: Int): Int = max(0, 100 - observationCount.coerceAtLeast(0) * 5)

@Composable
fun ReportListScreen(
    items: List<ReportListItemUiModel>,
    loading: Boolean,
    errorMessage: String?,
    onOpenReport: (String) -> Unit,
    onRetry: () -> Unit,
    onTabSelected: (String) -> Unit,
) {
    var selectedInspectionId by rememberSaveable(items) {
        mutableStateOf(items.firstOrNull { it.inspectionId != null }?.inspectionId)
    }
    val selectedReport = items.firstOrNull { it.inspectionId == selectedInspectionId }
    val completedReportCount = items.count { it.status in setOf(ReportListStatus.COMPLETED, ReportListStatus.PARTIAL) }
    AppPageScaffold(
        title = "리포트 선택",
        selectedTab = AppTab.Report,
        bottomAction = {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                PrimaryButton(
                    "선택한 매물 리포트 확인하기",
                    onClick = { selectedReport?.inspectionId?.let(onOpenReport) },
                    enabled = selectedReport?.inspectionId != null,
                )
            }
        },
        onTabSelected = { tab ->
            onTabSelected(
                when (tab) {
                    AppTab.Home -> "home"
                    AppTab.Property -> "property"
                    AppTab.Report -> "report"
                    AppTab.Profile -> "profile"
                },
            )
        },
    ) {
        Text("어느 매물의 리포트를 볼까요?", color = Green, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("점검을 마친 매물을 선택하면 결과를 확인할 수 있어요.", color = Secondary, fontSize = 12.sp)
        StateBadge("완료 리포트 ${completedReportCount}개", Green)
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
            errorMessage != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoNotice(errorMessage, PaleOrange, Orange)
                SecondaryButton("다시 불러오기", onRetry)
            }
            items.isEmpty() -> InfoNotice("등록된 매물이 없어요. 매물을 등록하고 점검을 완료하면 리포트를 확인할 수 있어요.", PaleGreen, Green)
            else -> items.forEach { item ->
                ReportPropertyCard(
                    item = item,
                    selected = item.inspectionId != null && item.inspectionId == selectedInspectionId,
                    onClick = { item.inspectionId?.let { selectedInspectionId = it } },
                )
            }
        }
    }
}

@Composable
private fun ReportPropertyCard(item: ReportListItemUiModel, selected: Boolean, onClick: () -> Unit) {
    val available = item.inspectionId != null
    val statusLabel = when (item.status) {
        ReportListStatus.NONE -> "리포트 없음"
        ReportListStatus.PROCESSING -> "생성 중"
        ReportListStatus.COMPLETED -> "작성 완료"
        ReportListStatus.PARTIAL -> "부분 완료"
        ReportListStatus.FAILED -> "생성 실패"
    }
    val statusColor = when (item.status) {
        ReportListStatus.PARTIAL -> Orange
        ReportListStatus.FAILED -> Color(0xFFD33B2F)
        ReportListStatus.NONE -> Secondary
        else -> Green
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(78.dp).clickable(enabled = available, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) PaleGreen else Color.White),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Green else Border),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(PaleGreen, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(if (available) Icons.Outlined.Home else Icons.Outlined.RealEstateAgent, null, tint = Green, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(item.propertyName, color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(item.address, color = Secondary, fontSize = 10.sp)
                Text(item.detail, color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReportDetailScreen(
    nickname: String,
    onBack: () -> Unit,
    onOpenProperty: () -> Unit,
    uiModel: ReportDetailUiModel,
    onRetry: () -> Unit = {},
    onMarkObservationViewed: (String) -> Unit = {},
) {
    var selectedObservationId by rememberSaveable { mutableStateOf<String?>(null) }
    selectedObservationId?.let { observationId ->
        ReportEvidenceViewer(
            observations = uiModel.observations,
            initialObservationId = observationId,
            onClose = { selectedObservationId = null },
            onMarkReviewed = onMarkObservationViewed,
        )
        return
    }

    val showsPropertyAction = uiModel.status !in setOf(ReportDetailStatus.WAITING_FOR_ANALYSIS, ReportDetailStatus.GENERATING)
    AppPageScaffold(
        title = "리포트",
        onBack = onBack,
        scrollable = true,
        bottomAction = if (showsPropertyAction) {
            {
                Column(
                    Modifier.background(ReportBackground).padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (uiModel.status == ReportDetailStatus.ERROR) {
                        PrimaryButton("다시 시도", onRetry)
                        SecondaryButton("매물 상세로 돌아가기", onOpenProperty)
                    } else {
                        PrimaryButton("매물 상세로 돌아가기", onOpenProperty)
                    }
                }
            }
        } else null,
    ) {
        ReportHeader(uiModel)
        when (uiModel.status) {
            ReportDetailStatus.WAITING_FOR_ANALYSIS -> ProcessingReport(uiModel, waitingForAnalysis = true)
            ReportDetailStatus.GENERATING -> ProcessingReport(uiModel, waitingForAnalysis = false)
            ReportDetailStatus.COMPLETED -> CompletedReport(nickname, uiModel) { selectedObservationId = it.id }
            ReportDetailStatus.EMPTY -> EmptyReport(uiModel)
            ReportDetailStatus.PARTIAL -> PartialReport(nickname, uiModel) { selectedObservationId = it.id }
            ReportDetailStatus.ERROR -> ErrorReport(uiModel)
        }
    }
}

@Composable
private fun ReportHeader(uiModel: ReportDetailUiModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${uiModel.inspectionDate} · ${uiModel.visitLabel}", color = Secondary, fontSize = 11.sp)
            Text("${uiModel.propertyName}\n점검 리포트", color = DeepGreen, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
        }
        StateBadge(
            label = when (uiModel.status) {
                ReportDetailStatus.WAITING_FOR_ANALYSIS -> "분석 중"
                ReportDetailStatus.GENERATING -> "생성 중"
                ReportDetailStatus.COMPLETED -> "작성 완료"
                ReportDetailStatus.EMPTY -> "분석 완료"
                ReportDetailStatus.PARTIAL -> "부분 완료"
                ReportDetailStatus.ERROR -> "생성 실패"
            },
            color = when (uiModel.status) {
                ReportDetailStatus.ERROR -> Color(0xFFD33B2F)
                ReportDetailStatus.PARTIAL -> Orange
                else -> Green
            },
        )
    }
}

@Composable
private fun ProcessingReport(uiModel: ReportDetailUiModel, waitingForAnalysis: Boolean) {
    val progress = if (uiModel.totalPhotoCount == 0) 0f else uiModel.processedPhotoCount.toFloat() / uiModel.totalPhotoCount
    Column(Modifier.fillMaxWidth().padding(top = 34.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(76.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
            Icon(Icons.Outlined.HourglassTop, null, tint = DeepGreen, modifier = Modifier.size(22.dp))
        }
        Text(if (waitingForAnalysis) "사진을 분석하고 있어요" else "리포트를 만들고 있어요", color = DeepGreen, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            if (waitingForAnalysis) "업로드한 사진을 AI가 순서대로 확인하고 있어요.\n모든 사진 분석이 끝나면 리포트를 자동으로 만들어요."
            else "사진 분석 결과를 구역별 확인 필요 관찰로 정리하고 있어요.\n완료되면 리포트에서 근거 사진을 확인할 수 있어요.",
            color = Secondary, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
        )
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Green, trackColor = PaleGreen)
        Row(Modifier.fillMaxWidth()) {
            Text("사진 분석", color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${uiModel.processedPhotoCount} / ${uiModel.totalPhotoCount}장", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        InfoNotice(
            if (waitingForAnalysis) "AI 분석 서버가 실행 중이어야 처리가 계속돼요. 분석이 멈춰 있으면 서버 상태를 확인해 주세요."
            else "앱을 닫아도 서버에서 리포트 생성은 계속돼요. 잠시 후 다시 확인해 주세요.",
            PaleGreen, Green,
        )
    }
}

@Composable
private fun CompletedReport(nickname: String, uiModel: ReportDetailUiModel, onEvidenceClick: (ReportObservationUiModel) -> Unit) {
    Text("AI 사진 분석과 ${nickname}님의 촬영 기록을 바탕으로 정리했어요.", color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
    ReportSummaryCard(uiModel, "확인 필요 관찰 ${uiModel.observations.size}건을 찾았어요")
    ReportMetrics(uiModel)
    ObservationSections(uiModel.zones, onEvidenceClick)
    AiDisclaimer()
}

@Composable
private fun PartialReport(nickname: String, uiModel: ReportDetailUiModel, onEvidenceClick: (ReportObservationUiModel) -> Unit) {
    InfoNotice("일부 사진을 분석하지 못했어요. 완료된 ${uiModel.completedPhotoCount}장의 결과만 먼저 보여드려요.", PaleOrange, Orange)
    Text("AI 사진 분석과 ${nickname}님의 촬영 기록을 바탕으로 정리했어요.", color = Secondary, fontSize = 12.sp)
    ReportSummaryCard(uiModel, "확인 가능한 관찰 ${uiModel.observations.size}건이 있어요")
    ReportMetrics(uiModel)
    ObservationSections(uiModel.zones, onEvidenceClick)
    AiDisclaimer()
}

@Composable
private fun EmptyReport(uiModel: ReportDetailUiModel) {
    Column(Modifier.fillMaxWidth().padding(top = 34.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(72.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CheckCircle, null, tint = Green, modifier = Modifier.size(38.dp))
        }
        Text("사진 분석을 완료했어요", color = DeepGreen, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("현재 촬영 근거에서 확인 필요 관찰이\n생성되지 않았어요.", color = Secondary, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
    }
    ReportSummaryCard(uiModel, "확인 필요 관찰이 생성되지 않았어요")
    ReportMetrics(uiModel)
    InfoNotice("이 결과는 하자가 없음을 보장하지 않아요. 촬영되지 않은 부분과 작은 흔적은 직접 확인해 주세요.", PaleOrange, Orange)
}

@Composable
private fun ErrorReport(uiModel: ReportDetailUiModel) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(72.dp).background(Color(0xFFFCE9E7), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = Color(0xFFD33B2F), modifier = Modifier.size(38.dp))
        }
        Text("리포트를 만들지 못했어요", color = DeepGreen, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(uiModel.errorMessage ?: "잠시 후 다시 시도해 주세요.", color = Secondary, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Refresh, null, tint = Green, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("다시 시도해도 기존 촬영 사진은 중복 등록되지 않아요.", color = Green, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ReportSummaryCard(uiModel: ReportDetailUiModel, title: String) {
    Row(Modifier.fillMaxWidth().background(PaleGreen, RoundedCornerShape(18.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("리포트 요약", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = DeepGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (uiModel.scoreIsProvisional) "일부 사진 분석 실패로 현재 점수는 잠정값이에요."
                else "참고 점수는 확인 필요 관찰 1건당 5점씩 차감해 계산해요.",
                color = Secondary, fontSize = 9.sp, lineHeight = 13.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${uiModel.referenceScore}", color = Green, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("/ 100 참고 점수", color = Secondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ReportMetrics(uiModel: ReportDetailUiModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportMetric(uiModel.observations.size.toString(), "확인 필요\n관찰", PaleOrange, Orange, Modifier.weight(1f))
        ReportMetric(uiModel.completedPhotoCount.toString(), "분석 완료\n사진", PaleGreen, Green, Modifier.weight(1f))
        ReportMetric(uiModel.failedPhotoCount.toString(), "분석 실패\n사진", Color.White, if (uiModel.failedPhotoCount > 0) Orange else DeepGreen, Modifier.weight(1f))
    }
}

@Composable
private fun ObservationSections(zones: List<ReportZoneUiModel>, onEvidenceClick: (ReportObservationUiModel) -> Unit) {
    Text("구역별 확인 필요 관찰", color = DeepGreen, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
    Text("항목을 펼쳐 AI가 참고한 사진과 위치를 확인할 수 있어요.", color = Secondary, fontSize = 11.sp)
    zones.forEachIndexed { index, zone ->
        var expanded by rememberSaveable(zone.name) { mutableStateOf(index == 0) }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Border)) {
            Column {
                Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(zone.name, color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    StateBadge("${zone.observations.size}건", Orange)
                    Spacer(Modifier.width(6.dp))
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Green)
                }
                if (expanded) zone.observations.forEach { observation -> ObservationCard(observation) { onEvidenceClick(observation) } }
            }
        }
    }
}

@Composable
private fun ObservationCard(observation: ReportObservationUiModel, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFFAFAF7)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        EvidenceThumbnail(observation, Modifier.size(width = 104.dp, height = 82.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(observation.label, color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            StateBadge("AI 신뢰도 ${observation.confidencePercent}%", Orange)
            Text(observation.description, color = Secondary, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 3)
            Text("근거 사진 분석", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EvidenceThumbnail(observation: ReportObservationUiModel, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(PaleGreen)) {
        EvidenceWithBboxes(observation, Modifier.fillMaxSize(), selectedOnly = true, showLabels = false)
    }
}

@Composable
fun ReportEvidenceViewer(
    observations: List<ReportObservationUiModel>,
    initialObservationId: String,
    onClose: () -> Unit,
    onMarkReviewed: (String) -> Unit = {},
) {
    BackHandler(onBack = onClose)
    val initialIndex = observations.indexOfFirst { it.id == initialObservationId }.coerceAtLeast(0)
    var currentIndex by rememberSaveable(initialObservationId) { mutableIntStateOf(initialIndex) }
    val observation = observations.getOrNull(currentIndex) ?: return
    Scaffold(
        containerColor = DarkViewer,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(DarkViewer).statusBarsPadding().height(70.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Close, "근거 사진 닫기", tint = Color.White, modifier = Modifier.size(31.dp))
                }
                Text("근거 사진 분석", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Surface(color = Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(22.dp)) {
                    Text("${currentIndex + 1} / ${observations.size}장", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp))
                }
            }
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(ReportBackground, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(observation.label, color = DeepGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    StatusChip(if (observation.reviewed) "확인 완료" else "확인 권장", if (observation.reviewed) Green else Orange)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ViewerArrow(currentIndex > 0, Icons.Outlined.ArrowBack, "이전 근거 사진") { currentIndex-- }
                    Spacer(Modifier.width(12.dp))
                    ViewerArrow(currentIndex < observations.lastIndex, Icons.Outlined.ArrowForward, "다음 근거 사진") { currentIndex++ }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { onMarkReviewed(observation.id) },
                        enabled = !observation.reviewed,
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green, disabledContainerColor = PaleGreen, disabledContentColor = MutedGreen),
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (observation.reviewed) "검토 완료" else "검토 완료 처리", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { innerPadding ->
        EvidenceWithBboxes(
            observation = observation,
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color.Black),
            selectedOnly = false,
            showLabels = true,
        )
    }
}

@Composable
private fun ViewerArrow(enabled: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(50.dp).clickable(enabled = enabled, onClick = onClick), color = if (enabled) PaleGreen else Color(0xFFF2F3EF), shape = CircleShape) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = if (enabled) Green else Secondary.copy(alpha = 0.4f), modifier = Modifier.size(27.dp))
        }
    }
}

@Composable
private fun EvidenceWithBboxes(observation: ReportObservationUiModel, modifier: Modifier, selectedOnly: Boolean, showLabels: Boolean) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val evidence = observation.evidence
        val imageWidth = evidence.imageWidth.coerceAtLeast(1).toFloat()
        val imageHeight = evidence.imageHeight.coerceAtLeast(1).toFloat()
        val scale = min(maxWidth.value / imageWidth, maxHeight.value / imageHeight)
        val viewportWidth = (imageWidth * scale).dp
        val viewportHeight = (imageHeight * scale).dp
        val selected = evidence.boxes.filter { it.observationId == observation.id }
        val boxes = if (selectedOnly) selected else evidence.boxes.filterNot { it.observationId == observation.id } + selected
        Box(Modifier.size(viewportWidth, viewportHeight)) {
            EvidenceImage(observation, Modifier.fillMaxSize(), ContentScale.FillBounds)
            boxes.forEach { box ->
                val isSelected = box.observationId == observation.id
                val color = box.displayColor.toColorOr(if (isSelected) Orange else Green)
                val left = box.left.coerceIn(0f, imageWidth)
                val top = box.top.coerceIn(0f, imageHeight)
                val right = box.right.coerceIn(left, imageWidth)
                val bottom = box.bottom.coerceIn(top, imageHeight)
                Box(
                    Modifier.offset(x = (left * scale).dp, y = (top * scale).dp)
                        .size(width = ((right - left) * scale).dp, height = ((bottom - top) * scale).dp)
                        .border(if (isSelected) 3.dp else 2.dp, color),
                )
                if (showLabels) {
                    Text(
                        "${box.displayLabel} ${box.confidencePercent}%${if (isSelected) " (선택됨)" else ""}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.offset(x = (left * scale).dp, y = ((top * scale) - 23f).coerceAtLeast(0f).dp)
                            .background(color, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

private fun String.toColorOr(fallback: Color): Color = runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)

@Composable
private fun EvidenceImage(observation: ReportObservationUiModel, modifier: Modifier, contentScale: ContentScale) {
    val remoteBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = observation.evidence.imageUrl) {
        value = observation.evidence.imageUrl?.let { signedUrl ->
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(signedUrl).openConnection().apply { connectTimeout = 8_000; readTimeout = 8_000 }
                        .getInputStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    }
    val bitmap = remoteBitmap
    when {
        bitmap != null -> Image(bitmap = bitmap, contentDescription = "${observation.label} 근거 사진", modifier = modifier, contentScale = contentScale)
        observation.evidence.useSamplePlaceholder -> Image(
            painter = painterResource(observation.evidence.placeholderRes),
            contentDescription = "${observation.label} 근거 사진",
            modifier = modifier,
            contentScale = contentScale,
        )
        else -> Box(modifier.background(Color(0xFF202A24)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Image, null, tint = Color.White.copy(alpha = 0.72f))
                Text("근거 사진을 불러올 수 없어요", color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ReportMetric(value: String, label: String, background: Color, color: Color, modifier: Modifier) {
    Column(modifier = modifier.height(76.dp).background(background, RoundedCornerShape(14.dp)).padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Secondary, fontSize = 9.sp, lineHeight = 12.sp)
    }
}

@Composable
private fun InfoNotice(message: String, background: Color, iconColor: Color) {
    Row(Modifier.fillMaxWidth().background(background, RoundedCornerShape(14.dp)).padding(13.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.WarningAmber, null, tint = iconColor, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = DeepGreen, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun AiDisclaimer() {
    InfoNotice("AI 관찰은 하자 확정이나 계약 판단이 아니라 사진 근거를 정리한 결과예요. 표시된 위치를 직접 확인해 주세요.", PaleOrange, Orange)
}

@Composable
private fun ReportNotice(message: String, background: Color, iconColor: Color) {
    Row(Modifier.fillMaxWidth().background(background, RoundedCornerShape(15.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.WarningAmber, null, tint = iconColor, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = DeepGreen, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatusChip(label: String, color: Color, compact: Boolean = false) {
    Surface(color = color.copy(alpha = if (color == SuccessGreen) 1f else 0.12f), shape = RoundedCornerShape(if (compact) 7.dp else 9.dp)) {
        Text(
            label,
            color = if (color == SuccessGreen) Color.White else color,
            fontSize = if (compact) 11.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 4.dp else 6.dp),
        )
    }
}

@Composable
private fun ReportPrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White, disabledContainerColor = Color(0xFFDCE4DE), disabledContentColor = MutedGreen),
    ) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun ReportOutlineButton(label: String, onClick: () -> Unit, orange: Boolean = false) {
    val color = if (orange) Orange else Green
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.5.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun ThreeLeafClover(modifier: Modifier = Modifier, color: Color = Green) {
    Canvas(modifier.semantics { contentDescription = "세입세잎 세잎클로버" }) {
        val radius = size.minDimension * 0.22f
        val stroke = Stroke(width = size.minDimension * 0.09f)
        drawCircle(color, radius, Offset(size.width * 0.34f, size.height * 0.38f), style = stroke)
        drawCircle(color, radius, Offset(size.width * 0.66f, size.height * 0.38f), style = stroke)
        drawCircle(color, radius, Offset(size.width * 0.50f, size.height * 0.65f), style = stroke)
        drawLine(color, Offset(size.width * 0.51f, size.height * 0.72f), Offset(size.width * 0.72f, size.height * 0.94f), size.minDimension * 0.09f, StrokeCap.Round)
    }
}

private object ReportPreviewData {
    private val evidence = ReportEvidenceUiModel(
        id = "preview-evidence",
        useSamplePlaceholder = true,
        boxes = listOf(EvidenceBoxUiModel("preview-observation", 190f, 300f, 850f, 760f, "곰팡이 의심", "#F68B38", 76)),
    )
    private val observation = ReportObservationUiModel(
        id = "preview-observation",
        zone = "구역 확인 필요",
        label = "벽면 미세 갈라짐 추정",
        confidencePercent = 76,
        description = "구역을 확정하기 어려운 사진에서 미세 균열 후보가 관찰됐어요.",
        evidence = evidence,
    )
    val completed = ReportDetailUiModel(
        status = ReportDetailStatus.COMPLETED,
        propertyName = "역삼 래미안 102동 1504호",
        propertyAddress = "서울시 강남구 역삼동 123-45",
        inspectionDate = "2026.08.25",
        completedPhotoCount = 21,
        totalPhotoCount = 21,
        serverReferenceScore = 95,
        zones = listOf(ReportZoneUiModel("구역 확인 필요", listOf(observation))),
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ReportCompletedPreview() {
    ReportDetailScreen("사용자", {}, {}, ReportPreviewData.completed)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ReportGeneratingPreview() {
    ReportDetailScreen("사용자", {}, {}, ReportPreviewData.completed.copy(status = ReportDetailStatus.GENERATING, completedPhotoCount = 14))
}
