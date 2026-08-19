package com.seipseip.app.feature.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.InfoCard
import com.seipseip.app.feature.common.SectionTitle
import com.seipseip.app.feature.common.StateBadge

@Composable
fun ProfileScreen(
    nickname: String,
    onTabSelected: (String) -> Unit,
) {
    AppPageScaffold(
        title = "내 정보",
        selectedTab = AppTab.Profile,
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
        Text("${nickname}님", color = DeepGreen, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("세입세잎과 안전한 첫 자취를 준비해요", color = Secondary, fontSize = 13.sp)
        StateBadge("안심 패스 이용 중", Green)
        SectionTitle("내 계정과 점검 환경")
        InfoCard("내 매물 관리", "등록한 매물 5개", accent = PaleGreen)
        InfoCard("스마트 글라스 연결", "세잎 Glass 01 · 연결됨")
        InfoCard("알림 및 앱 설정", "점검 알림과 계정 설정")
    }
}