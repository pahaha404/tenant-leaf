package com.seipseip.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.PaleOrange
import com.seipseip.app.R
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.AppBottomNavigation

private val HomeBackground = Color(0xFFF6F4EF)
private val StartInspectionOrange = Color(0xFFF28A3A)
private val InspectionTips = listOf(
    "싱크대 아래 휴지로 누수를 확인해요.",
    "창문을 닫고 외풍을 확인해요.",
    "샤워기로 수압과 온수를 확인해요.",
    "콘센트에 충전기를 꽂아 확인해요.",
    "천장 모서리의 누수 흔적을 살펴봐요.",
    "관리비 포함 항목을 계약 전에 확인해요.",
)

@Composable
fun HomeScreen(
    onOpenProperties: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenRecentReport: () -> Unit,
    onOpenMagazine: () -> Unit,
    onOpenMagazineArticle: (String) -> Unit,
    onStartInspection: () -> Unit,
    onOpenChecklist: () -> Unit,
    onTabSelected: (String) -> Unit,
    processing: Boolean = false,
) {
    TenantLeafHomeLayout(
        selectedTab = AppTab.Home,
        onOpenProperties = onOpenProperties,
        onOpenReports = onOpenReports,
        onOpenRecentReport = onOpenRecentReport,
        onOpenMagazine = onOpenMagazine,
        onOpenMagazineArticle = onOpenMagazineArticle,
        onStartInspection = onStartInspection,
        onOpenChecklist = onOpenChecklist,
        onTabSelected = onTabSelected,
        processing = processing,
    )
}

@Composable
fun TenantLeafHomeLayout(
    selectedTab: AppTab,
    onOpenProperties: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenRecentReport: () -> Unit = onOpenReports,
    onOpenMagazine: () -> Unit,
    onOpenMagazineArticle: (String) -> Unit = { onOpenMagazine() },
    onTabSelected: (String) -> Unit,
    onStartInspection: () -> Unit = onOpenProperties,
    onOpenChecklist: () -> Unit = onOpenProperties,
    processing: Boolean = false,
) {
    Box(modifier = Modifier.fillMaxSize().background(HomeBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            HomeHeader(processing)
            GlassStatusCard()
            if (processing) ReportProcessingCard(onOpenReports) else StartInspectionCard(onStartInspection)
            HomeQuickActions(onOpenProperties, onOpenChecklist)
            RecentReportCard(onOpenRecentReport, processing)
            InspectionTipCard()
            MagazineSection(onOpenAll = onOpenMagazine, onOpenArticle = onOpenMagazineArticle)
        }
        AppBottomNavigation(
            selectedTab = selectedTab,
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
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HomeHeader(processing: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (processing) "리포트를 정리 중이에요" else "오늘도 안심되는 자취", color = Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (processing) "잠시만 기다려 주세요" else "세입세잎", color = DeepGreen, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.NotificationsNone, null, tint = Green, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun GlassStatusCard() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Green).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIcon(tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Meta Ray-Ban AI Glass · 연결됨", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text("촬영 준비 완료", color = Color(0xFFDCE9D6), fontSize = 10.sp)
        }
    }
}

@Composable
private fun GlassIcon(tint: Color) {
    Canvas(modifier = Modifier.size(27.dp)) {
        val stroke = Stroke(width = 2.dp.toPx())
        val lensWidth = size.width * .36f
        val lensHeight = size.height * .34f
        val top = size.height * .33f
        drawRoundRect(tint, Offset(0f, top), Size(lensWidth, lensHeight), androidx.compose.ui.geometry.CornerRadius(lensHeight / 2, lensHeight / 2), style = stroke)
        drawRoundRect(tint, Offset(size.width - lensWidth, top), Size(lensWidth, lensHeight), androidx.compose.ui.geometry.CornerRadius(lensHeight / 2, lensHeight / 2), style = stroke)
        drawLine(tint, Offset(lensWidth, top + lensHeight / 2), Offset(size.width - lensWidth, top + lensHeight / 2), strokeWidth = 2.dp.toPx())
        drawLine(tint, Offset(0f, top + lensHeight / 2), Offset(-size.width * .12f, top), strokeWidth = 2.dp.toPx())
        drawLine(tint, Offset(size.width, top + lensHeight / 2), Offset(size.width * 1.12f, top), strokeWidth = 2.dp.toPx())
    }
}
@Composable
private fun StartInspectionCard(onClick: () -> Unit) = HomeHeroCard(
    icon = Icons.Outlined.PlayArrow,
    title = "점검 시작하기",
    description = "등록한 매물을 고르고 바로 시작해요",
    onClick = onClick,
    background = StartInspectionOrange,
    contentColor = Color.White,
    iconBackground = Color.White.copy(alpha = .18f),
)

@Composable
private fun ReportProcessingCard(onClick: () -> Unit) = HomeHeroCard(
    icon = Icons.Outlined.Article,
    title = "리포트 정리 중",
    description = "촬영한 구역을 차례대로 분석하고 있어요",
    onClick = onClick,
)

@Composable
private fun HomeHeroCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    background: Color = PaleGreen,
    contentColor: Color = DeepGreen,
    iconBackground: Color = Color.White,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(background).clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(iconBackground), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (contentColor == Color.White) Color.White else Orange, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(description, color = if (contentColor == Color.White) Color.White.copy(alpha = .85f) else Secondary, fontSize = 11.sp)
        }
        Icon(Icons.Outlined.ArrowForward, null, tint = if (contentColor == Color.White) Color.White else Green, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HomeQuickActions(onOpenProperties: () -> Unit, onOpenChecklist: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(Modifier.weight(1f), Icons.Outlined.AddHome, "매물 등록하기", "직접 정보 입력", PaleGreen, onOpenProperties)
        QuickAction(Modifier.weight(1f), Icons.Outlined.Checklist, "체크리스트 확인", "방문 전 미리 보기", PaleOrange, onOpenChecklist)
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    background: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(142.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(31.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = Green, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(description, color = Secondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun RecentReportCard(onClick: () -> Unit, processing: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(41.dp).clip(RoundedCornerShape(13.dp)).background(PaleOrange), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Article, null, tint = Orange, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (processing) "하자 점검 및 리포트 작성 중" else "최근 점검 리포트", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (processing) "촬영한 사진을 분석하고 리포트를 작성 중이에요" else "망원동 리버뷰 · 2026. 08. 19 점검", color = Secondary, fontSize = 10.sp)
        }
        Icon(Icons.Outlined.ArrowForward, null, tint = Green, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun InspectionTipCard() {
    val pagerState = rememberPagerState(pageCount = { InspectionTips.size })

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            Row(
                modifier = Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp)).background(PaleOrange).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = Orange, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("오늘의 점검 팁", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text(InspectionTips[page], color = DeepGreen, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(InspectionTips.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (pagerState.currentPage == index) 6.dp else 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (pagerState.currentPage == index) Orange else Color(0xFFFFD8B7)),
                )
            }
        }
    }
}

@Composable
private fun MagazineSection(onOpenAll: () -> Unit, onOpenArticle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("자취 매거진", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("전체보기  ›", modifier = Modifier.clickable(onClick = onOpenAll), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        MagazineArticle(R.drawable.magazine_1, "생활 준비", "첫 자취생 필수템 체크리스트", onClick = { onOpenArticle("first_essentials") })
        MagazineArticle(R.drawable.magazine_2, "집 구하기", "집 볼 때 흔히 하는 5가지 실수", onClick = { onOpenArticle("home_viewing_mistakes") })
        MagazineArticle(R.drawable.magazine_3, "계약 전", "계약서 쓰기 전 반드시 확인할 것", onClick = { onOpenArticle("contract_checklist") })
    }
}

@Composable
private fun MagazineArticle(imageRes: Int, tag: String, title: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(15.dp)).background(Color.White).clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(imageRes), contentDescription = null, modifier = Modifier.size(width = 76.dp, height = 58.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tag, color = Orange, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Icon(Icons.Outlined.ArrowForward, null, tint = Green, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun HomeBottomNavigation(selectedTab: AppTab, onTabSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(72.dp).background(Color.White).padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        HomeTab(Icons.Outlined.Home, "홈", selectedTab == AppTab.Home) { onTabSelected("home") }
        HomeTab(Icons.Outlined.RealEstateAgent, "매물", selectedTab == AppTab.Property) { onTabSelected("property") }
        HomeTab(Icons.Outlined.Article, "리포트", selectedTab == AppTab.Report) { onTabSelected("report") }
        HomeTab(Icons.Outlined.Person, "내 정보", selectedTab == AppTab.Profile) { onTabSelected("profile") }
    }
}

@Composable
private fun HomeTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.width(58.dp).clip(RoundedCornerShape(14.dp)).clickable(enabled = !selected, onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, tint = if (selected) Green else Secondary, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) Green else Secondary, fontSize = 9.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal)
    }
}
