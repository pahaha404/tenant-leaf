package com.seipseip.app.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.R
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppTab

private val HomeBackground = Color(0xFFFCFBF8)
private val HomeLightGreen = Color(0xFFEEF4EA)
private val HomeOrangeLight = Color(0xFFFFF0E4)
private val HomeGlassText = Color(0xFFDCE9D6)

@Composable
fun HomeScreen(
    onOpenProperties: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenMagazine: () -> Unit,
    onOpenMagazineArticle: () -> Unit,
    onStartInspection: () -> Unit,
    onTabSelected: (String) -> Unit,
) {
    val glassViewModel: GlassConnectionViewModel = viewModel()
    val glassState by glassViewModel.uiState.collectAsState()
    val activity = LocalContext.current as? Activity
    TenantLeafHomeLayout(
        selectedTab = AppTab.Home,
        onOpenProperties = onOpenProperties,
        onOpenReports = onOpenReports,
        onOpenMagazine = onOpenMagazine,
        onOpenMagazineArticle = onOpenMagazineArticle,
        onStartInspection = onStartInspection,
        glassState = glassState,
        onGlassClick = { activity?.let(glassViewModel::connect) },
        onTabSelected = onTabSelected,
    )
}

@Composable
fun TenantLeafHomeLayout(
    selectedTab: AppTab,
    onOpenProperties: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenMagazine: () -> Unit,
    onOpenMagazineArticle: () -> Unit = onOpenMagazine,
    onTabSelected: (String) -> Unit,
    onStartInspection: () -> Unit = onOpenProperties,
    processing: Boolean = false,
    glassState: GlassConnectionUiState = GlassConnectionUiState(),
    onGlassClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize().background(if (processing) Color(0xFFF6F4EF) else HomeBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            HomeHeader(processing)
            GlassStatusCard(glassState, onGlassClick)
            if (processing) ReportProcessingCard(onOpenReports) else StartInspectionCard(onStartInspection)
            HomeQuickActions(onOpenProperties)
            RecentReportCard(onOpenReports, processing)
            InspectionTipCard()
            MagazineSection(onOpenAll = onOpenMagazine, onOpenArticle = onOpenMagazineArticle)
        }
        HomeBottomNavigation(selectedTab, onTabSelected, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HomeHeader(processing: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (processing) "리포트를 정리 중이에요" else "오늘도 안심되는 자취", color = Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (processing) "잠시만 기다려 주세요" else "세입세잎", color = DeepGreen, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.NotificationsNone, null, tint = Green, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun GlassStatusCard(state: GlassConnectionUiState, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DeepGreen).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
            Text("◌", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(state.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(state.detail, color = HomeGlassText, fontSize = 10.sp)
        }
        Icon(Icons.Outlined.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StartInspectionCard(onClick: () -> Unit) = HomeHeroCard(
    icon = Icons.Outlined.PlayArrow,
    title = "점검 시작하기",
    description = "등록한 매물을 고르고 바로 시작해요",
    onClick = onClick,
)

@Composable
private fun ReportProcessingCard(onClick: () -> Unit) = HomeHeroCard(
    icon = Icons.Outlined.Article,
    title = "리포트 정리 중",
    description = "촬영한 구역을 차례대로 분석하고 있어요",
    onClick = onClick,
)

@Composable
private fun HomeHeroCard(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(HomeLightGreen).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(45.dp).clip(RoundedCornerShape(14.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Orange, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DeepGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(description, color = Secondary, fontSize = 10.sp)
        }
        Icon(Icons.Outlined.ArrowForward, null, tint = Green, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HomeQuickActions(onOpenProperties: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(Modifier.weight(1f), Icons.Outlined.AddHome, "매물 등록하기", "직접 정보 입력", PaleGreen, onOpenProperties)
        QuickAction(Modifier.weight(1f), Icons.Outlined.Checklist, "체크리스트 확인", "방문 전 미리 보기", HomeOrangeLight, onOpenProperties)
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
        Box(modifier = Modifier.size(41.dp).clip(RoundedCornerShape(13.dp)).background(HomeOrangeLight), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Article, null, tint = Orange, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (processing) "점검 리포트 정리 중" else "최근 점검 리포트", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (processing) "망원동 리버뷰 · 74% 완료" else "망원동 리버뷰 · 확인 필요 3건", color = Secondary, fontSize = 10.sp)
        }
        Icon(Icons.Outlined.ArrowForward, null, tint = Green, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun InspectionTipCard() {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(HomeOrangeLight).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Lightbulb, null, tint = Orange, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("오늘의 점검 팁", color = Color(0xFF8B542D), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text("싱크대 아래는 휴지로 쓸어보면 물기를 찾기 쉬워요.", color = Color(0xFF8B542D), fontSize = 10.sp)
        }
    }
}

@Composable
private fun MagazineSection(onOpenAll: () -> Unit, onOpenArticle: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("자취 매거진", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("전체보기  ›", modifier = Modifier.clickable(onClick = onOpenAll), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        MagazineArticle(R.drawable.magazine_1, "생활 꿀팁", "첫 자취생 필수템 체크리스트", onOpenArticle)
        MagazineArticle(R.drawable.magazine_2, "집 구하기", "집 볼 때 흔히 하는 5가지 실수", onOpenArticle)
        MagazineArticle(R.drawable.magazine_3, "계약 전", "계약서 쓰기 전 반드시 확인할 것", onOpenArticle)
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
    Column(modifier = Modifier.width(58.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, tint = if (selected) Green else Secondary, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) Green else Secondary, fontSize = 9.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal)
    }
}
