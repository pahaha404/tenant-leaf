package com.seipseip.app.feature.report

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.max
import kotlin.math.min
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val useSamplePlaceholder: Boolean = true,
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
)

data class ReportZoneUiModel(val name: String, val observations: List<ReportObservationUiModel>)

data class ReportDetailUiModel(
    val status: ReportDetailStatus,
    val propertyName: String,
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

fun reportReferenceScore(observationCount: Int): Int = max(0, 100 - observationCount.coerceAtLeast(0) * 5)

object ReportSamples {
    private val evidence = ReportEvidenceUiModel(
        id = "evidence-bath-01",
        boxes = listOf(
            EvidenceBoxUiModel(
                observationId = "observation-mold",
                left = 194f,
                top = 317f,
                right = 842f,
                bottom = 749f,
                displayLabel = "곰팡이 추정 흔적",
                displayColor = "#FF8A34",
                confidencePercent = 76,
            ),
        ),
        pageLabel = "1 / 3",
    )
    private val observations = listOf(
        ReportObservationUiModel(
            id = "observation-mold",
            zone = "화장실",
            label = "곰팡이 추정 흔적",
            confidencePercent = 76,
            description = "천장과 벽이 만나는 부분에서 곰팡이로 보이는 흔적이 관찰됐어요.",
            evidence = evidence,
        ),
        ReportObservationUiModel(
            id = "observation-water",
            zone = "화장실",
            label = "누수 추정 흔적",
            confidencePercent = 68,
            description = "배관 주변의 변색 부분을 직접 확인해 주세요.",
            evidence = evidence.copy(id = "evidence-bath-02", pageLabel = "2 / 3"),
        ),
        ReportObservationUiModel(
            id = "observation-window",
            zone = "창틀·환기",
            label = "표면 균열 추정 흔적",
            confidencePercent = 71,
            description = "창틀 가까운 벽면의 가는 선을 직접 확인해 주세요.",
            evidence = evidence.copy(id = "evidence-window-01", placeholderRes = R.drawable.guide_window_mold, pageLabel = "3 / 3"),
        ),
    )

    val completed = ReportDetailUiModel(
        status = ReportDetailStatus.COMPLETED,
        propertyName = "망원동 리버뷰",
        inspectionDate = "2026.08.24",
        completedPhotoCount = 21,
        totalPhotoCount = 21,
        zones = observations.groupBy(ReportObservationUiModel::zone).map { (zone, items) -> ReportZoneUiModel(zone, items) },
    )
    val generating = completed.copy(status = ReportDetailStatus.GENERATING, completedPhotoCount = 14, zones = emptyList())
    val empty = completed.copy(status = ReportDetailStatus.EMPTY, zones = emptyList())
    val partial = completed.copy(status = ReportDetailStatus.PARTIAL, completedPhotoCount = 19, failedPhotoCount = 2, zones = completed.zones.take(1))
    val error = completed.copy(
        status = ReportDetailStatus.ERROR,
        completedPhotoCount = 0,
        failedPhotoCount = 21,
        zones = emptyList(),
        errorMessage = "사진 분석 결과를 불러오지 못했어요. 네트워크 상태를 확인한 뒤 다시 시도해 주세요.",
    )
}

@Composable
fun ReportListScreen(onOpenReport: () -> Unit, onTabSelected: (String) -> Unit) {
    var selectedProperty by remember { mutableStateOf("망원동 리버뷰") }
    val selectedHasReport = selectedProperty == "망원동 리버뷰"
    AppPageScaffold(
        title = "리포트 선택",
        selectedTab = AppTab.Report,
        bottomAction = {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                PrimaryButton("선택한 매물 리포트 확인하기", onOpenReport, enabled = selectedHasReport)
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
        StateBadge("완료 리포트 3개", Green)
        ReportPropertyCard("망원동 리버뷰", "서울 마포구 망원동", "2026.08.24 · 확인 필요 관찰 3건", selectedProperty == "망원동 리버뷰", true) { selectedProperty = "망원동 리버뷰" }
        ReportPropertyCard("연남동 햇살 원룸", "서울 마포구 연남동", "아직 점검을 시작하지 않았어요", false, false) { selectedProperty = "연남동 햇살 원룸" }
        ReportPropertyCard("성산동 테라스 하우스", "서울 마포구 성산동", "아직 점검을 시작하지 않았어요", false, false) { selectedProperty = "성산동 테라스 하우스" }
    }
}

@Composable
private fun ReportPropertyCard(name: String, address: String, detail: String, selected: Boolean, available: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(78.dp).clickable(onClick = onClick),
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
                    Text(name, color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Text(if (available) "작성 완료" else "리포트 없음", color = if (available) Green else Secondary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(address, color = Secondary, fontSize = 10.sp)
                Text(detail, color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReportDetailScreen(
    nickname: String,
    onBack: () -> Unit,
    onOpenProperty: () -> Unit,
    uiModel: ReportDetailUiModel = ReportSamples.completed,
    onRetry: () -> Unit = {},
) {
    var selectedEvidence by remember { mutableStateOf<ReportObservationUiModel?>(null) }
    selectedEvidence?.let { observation ->
        ReportEvidenceViewer(observation = observation, onClose = { selectedEvidence = null })
        return
    }

    val showsPropertyAction = uiModel.status !in setOf(
        ReportDetailStatus.WAITING_FOR_ANALYSIS,
        ReportDetailStatus.GENERATING,
    )
    AppPageScaffold(
        title = "리포트",
        onBack = onBack,
        scrollable = true,
        bottomAction = if (showsPropertyAction) {
            {
                Column(
                    Modifier.background(Color(0xFFFCFBF8)).padding(horizontal = 20.dp, vertical = 10.dp),
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
            ReportDetailStatus.GENERATING -> GeneratingReport(uiModel)
            ReportDetailStatus.COMPLETED -> CompletedReport(nickname, uiModel) { selectedEvidence = it }
            ReportDetailStatus.EMPTY -> EmptyReport(uiModel)
            ReportDetailStatus.PARTIAL -> PartialReport(nickname, uiModel) { selectedEvidence = it }
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
private fun GeneratingReport(uiModel: ReportDetailUiModel) {
    ProcessingReport(uiModel, waitingForAnalysis = false)
}

@Composable
private fun ProcessingReport(uiModel: ReportDetailUiModel, waitingForAnalysis: Boolean) {
    val progress = if (uiModel.totalPhotoCount == 0) 0f else uiModel.processedPhotoCount.toFloat() / uiModel.totalPhotoCount
    Column(Modifier.fillMaxWidth().padding(top = 34.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(76.dp).background(PaleGreen, CircleShape), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
            Icon(Icons.Outlined.HourglassTop, null, tint = DeepGreen, modifier = Modifier.size(22.dp))
        }
        Text(
            if (waitingForAnalysis) "사진을 분석하고 있어요" else "리포트를 만들고 있어요",
            color = DeepGreen,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            if (waitingForAnalysis) {
                "업로드한 사진을 AI가 순서대로 확인하고 있어요.\n모든 사진 분석이 끝나면 리포트를 자동으로 만들어요."
            } else {
                "사진 분석 결과를 구역별 확인 필요 관찰로 정리하고 있어요.\n완료되면 리포트에서 근거 사진을 확인할 수 있어요."
            },
            color = Secondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Green, trackColor = PaleGreen)
        Row(Modifier.fillMaxWidth()) {
            Text("사진 분석", color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${uiModel.processedPhotoCount} / ${uiModel.totalPhotoCount}장", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        InfoNotice(
            if (waitingForAnalysis) {
                "AI 분석 서버가 실행 중이어야 처리가 계속돼요. 분석이 멈춰 있으면 서버 상태를 확인해 주세요."
            } else {
                "앱을 닫아도 서버에서 리포트 생성은 계속돼요. 잠시 후 다시 확인해 주세요."
            },
            PaleGreen,
            Green,
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
                color = Secondary,
                fontSize = 9.sp,
                lineHeight = 13.sp,
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
            Text("근거 사진 크게 보기", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
fun ReportEvidenceViewer(observation: ReportObservationUiModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Scaffold(containerColor = Color(0xFF101713)) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "근거 사진 닫기", tint = Color.White) }
                Text("근거 사진", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Text(observation.evidence.pageLabel, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            }
            EvidenceWithBboxes(
                observation = observation,
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(16.dp)),
                selectedOnly = false,
                showLabels = true,
            )
            Column(Modifier.fillMaxWidth().background(Color(0xFF1C2A22), RoundedCornerShape(16.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Image, null, tint = Orange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(observation.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    StateBadge("${observation.confidencePercent}%", Orange)
                }
                Text("${observation.zone} · AI가 표시한 영역", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                Text(observation.description, color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp, lineHeight = 17.sp)
                Text("AI 표시 영역과 신뢰도는 하자 확정이 아닌 직접 확인을 돕기 위한 참고 정보예요.", color = Color(0xFFFFC89E), fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun EvidenceWithBboxes(
    observation: ReportObservationUiModel,
    modifier: Modifier,
    selectedOnly: Boolean,
    showLabels: Boolean,
) {
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
                        text = "${box.displayLabel} · ${box.confidencePercent}%",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.offset(
                            x = (left * scale).dp,
                            y = ((top * scale) - 20f).coerceAtLeast(0f).dp,
                        ).background(color, RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private fun String.toColorOr(fallback: Color): Color = runCatching {
    Color(android.graphics.Color.parseColor(this))
}.getOrDefault(fallback)

@Composable
private fun EvidenceImage(
    observation: ReportObservationUiModel,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val remoteBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = observation.evidence.imageUrl) {
        value = observation.evidence.imageUrl?.let { signedUrl ->
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(signedUrl).openConnection().apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                    }.getInputStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    }
    val bitmap = remoteBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "${observation.label} 근거 사진",
            modifier = modifier,
            contentScale = contentScale,
        )
    } else if (observation.evidence.useSamplePlaceholder) {
        Image(
            painter = painterResource(observation.evidence.placeholderRes),
            contentDescription = "${observation.label} 근거 사진",
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier.background(Color(0xFF202A24)), contentAlignment = Alignment.Center) {
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
