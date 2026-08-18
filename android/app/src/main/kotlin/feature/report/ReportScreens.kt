package com.seipseip.app.feature.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.Border
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.PaleOrange
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.InfoCard
import com.seipseip.app.feature.common.PrimaryButton
import com.seipseip.app.feature.common.SectionTitle
import com.seipseip.app.feature.common.StateBadge

@Composable
fun ReportListScreen(
    onOpenReport: () -> Unit,
    onTabSelected: (String) -> Unit,
) {
    var selectedProperty by remember { mutableStateOf("망원동 리버뷰") }
    AppPageScaffold(
        title = "리포트 선택",
        selectedTab = AppTab.Report,
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
        ReportPropertyCard(
            name = "망원동 리버뷰",
            address = "서울 마포구 망원동",
            detail = "2026.08.13 · 직접 확인 24 / 28",
            selected = selectedProperty == "망원동 리버뷰",
            available = true,
            onClick = { selectedProperty = "망원동 리버뷰" },
        )
        ReportPropertyCard(
            name = "연남동 햇살 원룸",
            address = "서울 마포구 연남동",
            detail = "아직 점검을 시작하지 않았어요",
            selected = false,
            available = false,
            onClick = { },
        )
        ReportPropertyCard(
            name = "성산동 테라스 하우스",
            address = "서울 마포구 성산동",
            detail = "아직 점검을 시작하지 않았어요",
            selected = false,
            available = false,
            onClick = { },
        )
        PrimaryButton("선택한 매물 리포트 확인하기", onOpenReport)
    }
}

@Composable
private fun ReportPropertyCard(
    name: String,
    address: String,
    detail: String,
    selected: Boolean,
    available: Boolean,
    onClick: () -> Unit,
) {
    val cardColor = if (selected) Color(0xFFEEF4EA) else Color.White
    Card(
        modifier = Modifier.fillMaxWidth().height(78.dp).clickable(enabled = available, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (selected) BorderStroke(2.dp, Green) else BorderStroke(1.dp, Border),
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).background(PaleGreen, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(if (available) Icons.Outlined.Home else Icons.Outlined.RealEstateAgent, null, tint = Green, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(name, color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Text(if (available) "아파트" else "리포트 없음", color = if (available) Green else Secondary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(address, color = Secondary, fontSize = 10.sp)
                Text(detail, color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReportDetailScreen(
    onBack: () -> Unit,
    onOpenProperty: () -> Unit,
) {
    AppPageScaffold(title = "리포트", onBack = onBack) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("2026.08.13 · 첫 방문 점검", color = Secondary, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            StateBadge("작성 완료", Green)
        }
        Text("망원동 리버뷰\n집 리포트", color = Green, fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
        Text("AI 글래스 촬영 기록과 민지님의 직접 확인을 바탕으로 정리했어요.", color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
        Column(
            modifier = Modifier.fillMaxWidth().background(PaleGreen, RoundedCornerShape(16.dp)).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("세입세잎 한줄 의견", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text("확인 후 계약을 추천해요", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("욕실 수압과 창문 잠금만 다시 확인하면 좋아요.", color = Secondary, fontSize = 11.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportMetric("3", "확인 필요\n관찰", PaleOrange, Orange, Modifier.weight(1f))
            ReportMetric("24", "직접 확인\n완료", PaleGreen, Green, Modifier.weight(1f))
            ReportMetric("24", "사진\n근거", Color.White, DeepGreen, Modifier.weight(1f))
        }
        Text("계약 전 다시 볼 부분", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        ReportConcernRow("욕실 수압 · 온수", "재방문 때 수압과 온수 전환을 한 번 더 확인해요.")
        ReportConcernRow("창문 개폐 · 잠금", "창문 잠금장치가 헐겁지 않은지 다시 확인해요.")
        ReportConcernRow("욕실 환풍기", "작동 소리와 흡입 상태를 직접 확인해요.")
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF0E4), RoundedCornerShape(13.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("i", color = Orange, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(8.dp))
            Text("AI 관찰은 하자 확정이 아닌 사진 근거 정리예요. 계약 판단은 직접 확인 후 결정하세요.", color = Color(0xFF8B542D), fontSize = 10.sp, lineHeight = 14.sp)
        }
        Text("아래로 더 읽으며 구역별 근거 사진을 확인할 수 있어요 ↓", color = Secondary, fontSize = 10.sp)
        PrimaryButton("매물 상세로 돌아가기", onOpenProperty)
    }
}

@Composable
private fun ReportMetric(value: String, label: String, background: Color, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier.height(72.dp).background(background, RoundedCornerShape(13.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Secondary, fontSize = 9.sp, lineHeight = 12.sp)
    }
}

@Composable
private fun ReportConcernRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(13.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(31.dp).background(PaleOrange, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text("!", color = Orange, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(description, color = Secondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}