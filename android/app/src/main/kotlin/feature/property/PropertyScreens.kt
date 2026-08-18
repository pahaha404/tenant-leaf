package com.seipseip.app.feature.property

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.InfoCard
import com.seipseip.app.feature.common.PrimaryButton
import com.seipseip.app.feature.common.SectionTitle
import com.seipseip.app.feature.common.StateBadge

@Composable
fun PropertyListScreen(
    onAddProperty: () -> Unit,
    onOpenProperty: () -> Unit,
    onTabSelected: (String) -> Unit,
) {
    AppPageScaffold(
        title = "매물",
        selectedTab = AppTab.Property,
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
        Text("점검할 매물을 관리해요", color = Secondary, fontSize = 13.sp)
        StateBadge("등록 매물 1개")
        PropertyCard(onClick = onOpenProperty)
        PrimaryButton("새 매물 등록", onAddProperty)
        InfoCard(
            title = "방문 전 확인",
            description = "방문 일정과 주소를 미리 확인하면 점검 준비가 쉬워요.",
            accent = PaleGreen,
        )
    }
}

@Composable
fun PropertyFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    var propertyName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var housingType by remember { mutableStateOf("원룸") }
    var recordMethod by remember { mutableStateOf("스마트 글라스") }

    AppPageScaffold(title = "매물 등록", onBack = onBack) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("점검할 방을 알려주세요", color = Green, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            StateBadge("1 / 3", Green)
        }
        Text("기본 정보는 리포트 제목과 증거 정리에 사용돼요.", color = Secondary, fontSize = 13.sp)
        Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)).background(PaleGreen)) {
            Box(modifier = Modifier.fillMaxWidth(0.31f).height(7.dp).clip(RoundedCornerShape(99.dp)).background(Green))
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FormTextField("매물 이름", "예: 연남동 햇살 원룸", propertyName, { propertyName = it })
            FormTextField("주소", "도로명이나 건물명을 검색하세요", address, { address = it })
            Text("주거 형태", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HousingOption("원룸", housingType, { housingType = it }, Modifier.weight(1f))
                HousingOption("오피스텔", housingType, { housingType = it }, Modifier.weight(1f))
                HousingOption("투룸 이상", housingType, { housingType = it }, Modifier.weight(1f))
            }
            Text("방문 예정일", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).clickable { }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("2025. 08. 16 (토)  14:00", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("⌄", color = Secondary, fontSize = 18.sp)
            }
            Text("현장 기록 준비", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            RecordMethodOption("⌁", "스마트 글라스", "안경으로 촬영하고 음성으로 메모해요", recordMethod == "스마트 글라스") { recordMethod = "스마트 글라스" }
            RecordMethodOption("◉", "휴대폰으로 기록", "휴대전화 카메라로 촬영해요", recordMethod == "휴대폰") { recordMethod = "휴대폰" }
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF0E4)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✦", color = Orange, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(9.dp))
            Text("점검 중 놓치기 쉬운 흔적은 AI가 함께 관찰해요.", color = Color(0xFF78472C), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        PrimaryButton("체크리스트 선택하기", onSaved)
    }
}

@Composable
private fun FormTextField(label: String, placeholder: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            placeholder = { Text(placeholder, color = Secondary, fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
    }
}

@Composable
private fun HousingOption(label: String, selected: String, onSelect: (String) -> Unit, modifier: Modifier) {
    val isSelected = label == selected
    Box(
        modifier = modifier.height(42.dp).clip(RoundedCornerShape(99.dp)).background(if (isSelected) Green else Color.White).clickable { onSelect(label) },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (isSelected) Color.White else Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecordMethodOption(icon: String, title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) PaleGreen else Color.White).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, color = if (selected) Green else Secondary, fontSize = 21.sp)
        Spacer(Modifier.width(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = if (selected) Green else DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Secondary, fontSize = 10.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(if (selected) "●" else "○", color = if (selected) Green else Secondary, fontSize = 18.sp)
    }
}
@Composable
fun PropertyDetailScreen(
    onBack: () -> Unit,
    onStartInspection: () -> Unit,
    onOpenReport: () -> Unit,
    onTabSelected: (String) -> Unit,
) {
    AppPageScaffold(
        title = "매물 상세",
        onBack = onBack,
        selectedTab = AppTab.Property,
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
        Text("망원동 리버뷰", color = DeepGreen, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("서울시 마포구 망원동 · 원룸", color = Secondary, fontSize = 13.sp)
        StateBadge("점검 준비 가능")
        InfoCard("기본 정보", "보증금 1,000만 원 · 월세 65만 원 · 관리비 7만 원")
        InfoCard(
            title = "최근 점검 기록",
            description = "2026.08.18 · 주방과 창틀은 확인이 필요해요.",
            onClick = onOpenReport,
        )
        SectionTitle("다음 방문")
        InfoCard("방문 예정", "오늘 오후 4:00 · 점검 전 튜토리얼을 확인해요.", accent = PaleGreen)
        PrimaryButton("방문 점검 준비하기", onStartInspection)
    }
}

@Composable
fun PropertySelectScreen(
    onBack: () -> Unit,
    onSelected: () -> Unit,
) {
    AppPageScaffold(title = "점검할 매물 선택", onBack = onBack) {
        SectionTitle("어느 매물을 점검할까요?", "점검 기록은 선택한 매물에 저장돼요.")
        PropertyCard(onClick = onSelected)
        InfoCard(
            title = "다른 매물이 없나요?",
            description = "새 매물을 등록하면 방문 준비와 점검을 바로 시작할 수 있어요.",
        )
        PrimaryButton("선택한 매물로 계속하기", onSelected)
    }
}

@Composable
private fun PropertyCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("망원동 리버뷰", color = DeepGreen, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                StateBadge("점검 예정", Green)
            }
            Text("서울시 마포구 망원동 · 원룸", color = Secondary, fontSize = 12.sp)
            Text("방문 일정  오늘 오후 4:00", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}