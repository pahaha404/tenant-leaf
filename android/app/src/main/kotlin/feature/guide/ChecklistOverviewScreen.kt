package com.seipseip.app.feature.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.GuideZone
import com.seipseip.app.feature.common.UiCatalog

@Composable
fun ChecklistOverviewScreen(
    onBack: () -> Unit,
    onOpenZone: (String) -> Unit,
) {
    AppPageScaffold(title = "체크리스트", onBack = onBack) {
        Text(
            "방문 전 체크리스트",
            color = DeepGreen,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            "구역을 누르면 사진 예시와 확인 방법을 미리 볼 수 있어요.",
            color = Secondary,
            fontSize = 12.sp,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UiCatalog.guideZones.forEachIndexed { index, zone ->
                ChecklistZoneRow(
                    icon = zoneIcon(zone.id),
                    isHighlighted = index % 2 != 0,
                    zone = zone,
                    onClick = { onOpenZone(zone.id) },
                )
            }
        }
    }
}

@Composable
private fun ChecklistZoneRow(
    icon: ImageVector,
    isHighlighted: Boolean,
    zone: GuideZone,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isHighlighted) Color(0xFFFFF4E8) else Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PaleGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "${zone.title} 아이콘",
                tint = Green,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(zone.title, color = DeepGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text("${zone.items.size}개 항목 · ${zone.subtitle}", color = Secondary, fontSize = 11.sp)
        }
        Text("›", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Medium)
    }
}
private fun zoneIcon(zoneId: String): ImageVector = when (zoneId) {
    "entry" -> Icons.Outlined.MeetingRoom
    "kitchen" -> Icons.Outlined.Kitchen
    "window" -> Icons.Outlined.Window
    "room" -> Icons.Outlined.Weekend
    "bathroom" -> Icons.Outlined.Bathtub
    else -> Icons.Outlined.MeetingRoom
}