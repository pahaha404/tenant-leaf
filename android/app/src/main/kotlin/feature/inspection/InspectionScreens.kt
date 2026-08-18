package com.seipseip.app.feature.inspection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.LinearProgressIndicator
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
import com.seipseip.app.PaleOrange
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.InfoCard
import com.seipseip.app.feature.common.PrimaryButton
import com.seipseip.app.feature.common.SectionTitle
import com.seipseip.app.feature.common.StateBadge
import com.seipseip.app.feature.common.UiCatalog

@Composable
fun InspectionPrepScreen(
    onBack: () -> Unit,
    onStartInspection: () -> Unit,
    onSelectProperty: () -> Unit,
) {
    AppPageScaffold(title = "점검 준비", onBack = onBack) {
        SectionTitle("점검을 시작하기 전이에요", "망원동 리버뷰 · 오늘 오후 4:00")
        InfoCard(
            title = "점검할 매물",
            description = "망원동 리버뷰 · 서울시 마포구 망원동",
            onClick = onSelectProperty,
        )
        InfoCard(
            title = "촬영 전 확인",
            description = "휴대전화 카메라와 마이크 권한을 허용해 주세요.",
            accent = PaleGreen,
        )
        InfoCard(
            title = "세입세잎 Glass 연결",
            description = "연결하지 않아도 휴대전화 카메라로 점검을 진행할 수 있어요.",
        )
        PrimaryButton("점검 시작하기", onStartInspection)
    }
}

@Composable
fun TutorialScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    AppPageScaffold(title = "튜토리얼", onBack = onBack) {
        Text(
            "스마트 글래스로 한 번에!\n튜토리얼 영상 어쩌구",
            modifier = Modifier.fillMaxWidth().padding(top = 42.dp),
            color = Green,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "스마트 글래스 촬영부터 직접 확인까지\n가장 중요한 흐름만 빠르게 알려드려요.",
            modifier = Modifier.fillMaxWidth(),
            color = Secondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(width = 218.dp, height = 160.dp).clip(RoundedCornerShape(22.dp)).background(PaleGreen),
                contentAlignment = Alignment.Center,
            ) {
                Text("⌁", modifier = Modifier.align(Alignment.TopStart).padding(18.dp), color = Green, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(66.dp).clip(RoundedCornerShape(99.dp)).background(Green), contentAlignment = Alignment.Center) {
                    Text("▶", color = Color.White, fontSize = 25.sp)
                }
            }
        }
        StateBadge("◷  60초 영상", Orange)
        Spacer(Modifier.height(22.dp))
        PrimaryButton("▶  60초 영상 보기", onNext)
        Text(
            "건너뛰고 바로 시작하기",
            modifier = Modifier.fillMaxWidth().clickable(onClick = onNext).padding(vertical = 4.dp),
            color = Secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun TutorialChecklistScreen(
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onStart: () -> Unit,
) {
    AppPageScaffold(title = "점검 시작 전", onBack = onBack) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("이제 체크리스트를\n확인해 볼까요?", color = Green, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(22.dp))
            Text("2 / 2", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text("방을 둘러보며 놓치기 쉬운 부분을 하나씩 기록해요.", color = Secondary, fontSize = 13.sp)
        Card(
            modifier = Modifier.fillMaxWidth().height(210.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
                        Text("✓", color = Green, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("방문 점검 체크리스트", color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Text("총 28개 항목 · 약 15분", color = Secondary, fontSize = 11.sp)
                    }
                }
                ChecklistGuideLine("◉", "중요한 부분은 사진으로 남겨요")
                ChecklistGuideLine("✋", "최종 상태는 내가 직접 선택해요")
                ChecklistGuideLine("▣", "기록은 리포트로 한눈에 정리돼요")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF0E4)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✦", color = Orange, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text("AI 관찰은 촬영 근거를 돕고, 최종 확인은 사용자가 해요.", color = Color(0xFF8B542D), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton("체크리스트 확인하기", onOpenGuide)
        Text("바로 점검 시작하기", modifier = Modifier.fillMaxWidth().clickable(onClick = onStart).padding(vertical = 4.dp), color = Secondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ChecklistGuideLine(symbol: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = Green, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Secondary, fontSize = 11.sp)
    }
}
@Composable
fun LiveInspectionScreen(
    zoneId: String,
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onNextZone: (String) -> Unit,
    onFinish: () -> Unit,
) {
    val zone = UiCatalog.zone(zoneId)
    val nextZone = UiCatalog.nextZone(zoneId)
    val zoneRows = UiCatalog.guideZones
    var showFinishDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F4EF))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("‹", color = DeepGreen, fontSize = 28.sp, fontWeight = FontWeight.Medium) }
                Spacer(Modifier.width(12.dp))
                Text("실시간 점검", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFE7E2)).padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFC9573D)))
                    Spacer(Modifier.width(5.dp))
                    Text("녹화 중", color = Color(0xFFC9573D), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF2F4437)),
                ) {
                    Text("AI 글래스 카메라 프리뷰", modifier = Modifier.align(Alignment.Center), color = Color.White.copy(alpha = .8f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(13.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .92f)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⌖", color = Green, fontSize = 14.sp)
                        Spacer(Modifier.width(5.dp))
                        Text("현재 구역 · ${zone.title}", color = DeepGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("00:04:28", modifier = Modifier.align(Alignment.BottomEnd).padding(13.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
                        Text("◖", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("AI 음성 안내", color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("카메라를 천천히 움직이며 ${zone.title}을(를) 비춰주세요.", color = Secondary, fontSize = 10.sp)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(29.dp).clip(RoundedCornerShape(10.dp)).background(PaleGreen), contentAlignment = Alignment.Center) { Text("✓", color = Green, fontWeight = FontWeight.ExtraBold) }
                        Spacer(Modifier.width(8.dp))
                        Text("${zone.title} 확인 안내", color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("아래 항목을 하나씩 비추고, 이상이 있으면 가까이에서 한 번 더 촬영해 주세요.", color = Secondary, fontSize = 11.sp, lineHeight = 16.sp)
                    zone.items.take(4).forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (index == 0) PaleGreen else Color(0xFFF8F8F6)).clickable(onClick = onOpenGuide).padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (index == 0) "●" else "○", color = if (index == 0) Green else Secondary, fontSize = 11.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(item.title, color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("가이드", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("실시간 인식 구역", color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    zoneRows.forEach { itemZone ->
                        val isCurrent = itemZone.id == zoneId
                        Row(
                            modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(8.dp)).background(if (isCurrent) PaleGreen else Color.White).padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (isCurrent) "●" else "○", color = if (isCurrent) Green else Secondary, fontSize = 9.sp)
                            Spacer(Modifier.width(7.dp))
                            Text(itemZone.title, color = if (isCurrent) DeepGreen else Secondary, fontSize = 10.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                            if (isCurrent) {
                                Spacer(Modifier.weight(1f))
                                Text("촬영 중", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(Green).clickable {
                        if (nextZone != null) onNextZone(nextZone.id) else { showFinishDialog = true }
                    },
                    contentAlignment = Alignment.Center,
                ) { Text(if (nextZone == null) "촬영 기록 저장하기" else "촬영 계속하기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                Box(
                    modifier = Modifier.width(92.dp).height(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF0E4)).clickable { showFinishDialog = true },
                    contentAlignment = Alignment.Center,
                ) { Text("점검 종료", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
        if (showFinishDialog) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x8A173426)).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(PaleOrange),
                        contentAlignment = Alignment.Center,
                    ) { Text("■", color = Orange, fontSize = 18.sp) }
                    Text("정말 촬영을\n종료하시겠습니까?", color = DeepGreen, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("촬영을 종료하면 AI가 수집된 장면을 분석해 구역별 관찰 결과를 정리해요.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PaleGreen).padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("♧", color = Green, fontSize = 16.sp)
                        Spacer(Modifier.width(7.dp))
                        Text("분석이 끝나면 알림으로 알려드릴게요.", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(13.dp)).background(Color.White).border(1.dp, Color(0xFFD9E1DA), RoundedCornerShape(13.dp)).clickable { showFinishDialog = false },
                        contentAlignment = Alignment.Center,
                    ) { Text("아니요, 계속 촬영할게요", color = Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(13.dp)).background(Orange).clickable(onClick = onFinish),
                        contentAlignment = Alignment.Center,
                    ) { Text("네, 종료할게요", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }    }
}
@Composable
fun FinishConfirmScreen(
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppPageScaffold(title = "촬영 종료 확인", onBack = onBack) {
        SectionTitle("점검 촬영을 마칠까요?", "종료하면 촬영 기록을 정리하고 구역별 분석을 시작해요.")
        InfoCard(
            title = "촬영 구역",
            description = "현관·공용, 주방, 창틀·환기, 거실·방, 화장실",
            accent = PaleOrange,
        )
        InfoCard(
            title = "안내",
            description = "분석 결과는 확인이 필요한 관찰 결과이며, 최종 상태는 사용자가 결정해요.",
        )
        PrimaryButton("촬영 종료하고 분석 시작", onConfirm)
    }
}

@Composable
fun AnalysisProgressScreen(
    onBackToHome: () -> Unit,
    onOpenObservation: () -> Unit,
) {
    AppPageScaffold(title = "분석 진행", onBack = onBackToHome) {
        SectionTitle("구역별 기록을 정리하고 있어요", "촬영한 사진과 메모를 점검 구역별로 묶는 중이에요.")
        LinearProgressIndicator(
            progress = { 0.74f },
            modifier = Modifier.fillMaxWidth(),
            color = Green,
            trackColor = PaleGreen,
        )
        Text("74% · 주방과 창틀 기록을 확인 중이에요.", color = Secondary, fontSize = 12.sp)
        UiCatalog.guideZones.forEachIndexed { index, zone ->
            InfoCard(
                title = zone.title,
                description = if (index < 3) "기록 정리 완료" else "확인 중",
                accent = if (index < 3) PaleGreen else Color.White,
            )
        }
        PrimaryButton("구역별 관찰 결과 보기", onOpenObservation)
    }
}

@Composable
fun ObservationScreen(
    zoneId: String,
    onBack: () -> Unit,
    onNextZone: (String) -> Unit,
    onOpenReport: () -> Unit,
) {
    val zone = UiCatalog.zone(zoneId)
    val nextZone = UiCatalog.nextZone(zoneId)

    AppPageScaffold(title = "구역별 관찰", onBack = onBack) {
        SectionTitle(zone.title, "사진과 기록을 기반으로 확인이 필요한 부분을 정리했어요.")
        StateBadge("확인 필요", Orange)
        InfoCard(
            title = zone.title + " 관찰 결과",
            description = zone.items.first().title + " 주변에 추가 확인이 필요한 흔적이 있어요. 실제 상태는 현장에서 직접 확인해 주세요.",
            accent = PaleOrange,
        )
        InfoCard(
            title = "근거 사진",
            description = "촬영한 사진 3장 · 촬영 시각과 구역 정보가 함께 보관돼요.",
        )
        InfoCard(
            title = "재촬영 안내",
            description = "빛이 어두웠거나 흔들린 사진은 가까이에서 한 번 더 촬영해 주세요.",
        )
        if (nextZone != null) {
            PrimaryButton(label = nextZone.title + " 관찰 보기", onClick = { onNextZone(nextZone.id) })
        } else {
            PrimaryButton("점검 리포트 보기", onOpenReport)
        }
    }
}

@Composable
private fun CheckLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StateBadge("확인", Green)
        Text(text, modifier = Modifier.padding(start = 10.dp), color = DeepGreen, fontSize = 13.sp)
    }
}