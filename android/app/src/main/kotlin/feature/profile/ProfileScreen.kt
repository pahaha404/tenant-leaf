package com.seipseip.app.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onLogout: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    showBottomBar: Boolean = true,
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    AppPageScaffold(
        title = "내 정보",
        selectedTab = AppTab.Profile,
        showBottomBar = showBottomBar,
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

        Spacer(Modifier.height(8.dp))

        // Logout Card Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLogoutDialog = true },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "로그아웃",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "로그아웃",
                    color = Color(0xFFDC2626),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃할까요?", fontWeight = FontWeight.Bold) },
            text = { Text("로그아웃하면 로그인 화면으로 이동합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) {
                    Text("로그아웃", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = Secondary)
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
        )
    }
}