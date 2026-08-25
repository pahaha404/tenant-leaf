package com.seipseip.app.feature.state

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.Orange
import com.seipseip.app.R
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
        delay(2_000)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F4EF))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_seipseip),
            contentDescription = "세입세잎 캐릭터",
            modifier = Modifier.size(260.dp),
            contentScale = ContentScale.Fit,
        )
        androidx.compose.material3.Text(
            text = "세입세잎",
            modifier = Modifier.padding(top = 22.dp),
            color = Orange,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        androidx.compose.material3.Text(
            text = "초보 세입자를 위한 SAFE GUIDE",
            modifier = Modifier.padding(top = 8.dp),
            color = Color(0xFF7A795C),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
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
