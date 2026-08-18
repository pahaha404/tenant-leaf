package com.seipseip.app.feature.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.Secondary
import com.seipseip.app.feature.home.TenantLeafHomeLayout
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.EmptyState
import com.seipseip.app.feature.common.InfoCard
import com.seipseip.app.feature.common.SectionTitle
import com.seipseip.app.feature.common.StateBadge
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(550)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Green)
        Text(
            text = "세입세잎을 준비하고 있어요",
            modifier = Modifier.padding(top = 20.dp),
            color = DeepGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "내 매물과 점검 기록을 안전하게 불러오는 중이에요.",
            modifier = Modifier.padding(top = 8.dp),
            color = Secondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun HomeProcessingScreen(
    onOpenProperties: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenMagazine: () -> Unit,
    onTabSelected: (String) -> Unit,
) {
    TenantLeafHomeLayout(
        selectedTab = AppTab.Home,
        onOpenProperties = onOpenProperties,
        onOpenReports = onOpenReports,
        onOpenMagazine = onOpenMagazine,
        onTabSelected = onTabSelected,
        processing = true,
    )
}
@Composable
fun EmptyPropertyScreen(
    onBack: () -> Unit,
    onAddProperty: () -> Unit,
) {
    AppPageScaffold(title = "점검할 매물 선택", onBack = onBack) {
        EmptyState(
            title = "아직 등록한 매물이 없어요",
            description = "방문할 매물의 이름과 주소를 등록하면 점검을 시작할 수 있어요.",
            actionLabel = "매물 등록하기",
            onAction = onAddProperty,
        )
    }
}