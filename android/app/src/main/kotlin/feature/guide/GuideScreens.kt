package com.seipseip.app.feature.guide

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.R
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.GuideItem
import com.seipseip.app.feature.common.UiCatalog

private val GuideBackground = Color.White

@Composable
fun GuideZoneScreen(
    zoneId: String,
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onNextGuide: (String) -> Unit,
    onStartInspection: () -> Unit,
) {
    val zone = UiCatalog.zone(zoneId)
    val currentIndex = UiCatalog.guideZones.indexOfFirst { it.id == zoneId }.coerceAtLeast(0)
    val nextZone = UiCatalog.nextZone(zoneId)
    val headline = if (zoneId == "entry") {
        "첫 집 보기 전,\n현관부터 살펴봐요"
    } else {
        zoneGuideHeadline(zone.title)
    }
    val description = if (zoneId == "entry") "안전과 생활 편의를 결정하는 네 가지예요." else zone.subtitle
    val nextLabel = if (nextZone != null) "${nextZone.title} 가이드 보기" else "홈으로 가기"

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxWidth().height(230.dp).background(Green).padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("‹", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Medium) }
                Spacer(Modifier.weight(1f))
                Text(
                    "가이드 ${currentIndex + 1} / ${UiCatalog.guideZones.size}",
                    modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = .14f)).padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color(0xFFDCE9D6), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(modifier = Modifier.padding(top = 13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(headline, color = Color.White, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(description, color = Color(0xFFDCE9D6), fontSize = 12.sp)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("필수 확인 항목", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Text("사진과 함께 살펴보세요", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            zone.items.take(4).forEachIndexed { index, item ->
                GuideItemRow(item = item, number = index + 1, onClick = { onOpenDetail(index) })
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(10.dp)).background(PaleGreen).padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ⓘ", color = Green, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text("항목을 누르면 사진 예시와 확인 방법을 볼 수 있어요.", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).background(Orange).clickable {
                    if (nextZone != null) onNextGuide(nextZone.id) else onStartInspection()
                },
                contentAlignment = Alignment.Center,
            ) {
                Text("$nextLabel  ›", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun zoneGuideHeadline(title: String): String = when (title) {
    "주방" -> "주방부터\n하나씩 볼까요?"
    "창틀·환기" -> "창틀·환기도\n놓치지 말아요"
    "거실·방" -> "거실·방은\n천천히 둘러봐요"
    "화장실" -> "화장실도\n꼼꼼히 확인해요"
    else -> "${title}부터\n하나씩 볼까요?"
}

@Composable
fun GuideDetailScreen(
    zoneId: String,
    itemIndex: Int,
    onBack: () -> Unit,
    nextLabel: String,
    onNext: () -> Unit,
) {
    val zone = UiCatalog.zone(zoneId)
    val item = zone.items.getOrElse(itemIndex) { zone.items.first() }
    val steps = item.steps
    val isDoorGuide = zoneId == "entry" && itemIndex == 0
    val imageRes = guideImageResource(zoneId, itemIndex)
    val progress = "${zone.title} ${itemIndex + 1} / ${zone.items.size}"

    Column(modifier = Modifier.fillMaxSize().background(GuideBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = DeepGreen, fontSize = 25.sp, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.weight(1f))
            Text(
                progress,
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(PaleGreen).padding(horizontal = 9.dp, vertical = 5.dp),
                color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            )
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(PaleGreen), contentAlignment = Alignment.Center) {
                    Text(if (isDoorGuide) "⌂" else "✓", color = Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(item.title, color = Green, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(item.description, color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Box(modifier = Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(18.dp)).background(PaleGreen)) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = "${item.title} 사진 예시",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    "사진 예시",
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = .92f)).padding(horizontal = 8.dp, vertical = 5.dp),
                    color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                )

            }
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PaleGreen).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("이렇게 살펴보세요", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                steps.forEachIndexed { index, step -> GuideStep(index + 1, step) }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(13.dp)).background(Color.White).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("▣", color = Green, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text("이상한 부분이 보이면 가까이에서 한 장 더 남겨두세요.", color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 14.dp).clip(RoundedCornerShape(14.dp)).background(Orange).clickable {
                onNext()
            },
            contentAlignment = Alignment.Center,
        ) {
            Text("$nextLabel  ›", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun GuideTopBar(onBack: () -> Unit, progress: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Text("‹", color = DeepGreen, fontSize = 28.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.width(12.dp))
        Text("안심 가이드", color = DeepGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.weight(1f))
        Text(progress, color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun GuideItemRow(item: GuideItem, number: Int, onClick: () -> Unit) {
    val rowColor = if (number % 2 == 1) PaleGreen else Color(0xFFFFF8F1)
    val iconLabel = when (number) { 1 -> "⌂"; 2 -> "⌕"; 3 -> "▣"; else -> "◌" }
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(15.dp)).background(rowColor).clickable(onClick = onClick).padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(39.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .58f)), contentAlignment = Alignment.Center) {
            Text(iconLabel, color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.title, color = DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(item.description, color = Secondary, fontSize = 9.sp, lineHeight = 13.sp)
        }
        Text("›", color = Secondary, fontSize = 19.sp)
    }
}

internal fun guideImageResource(zoneId: String, itemIndex: Int): Int = when ("$zoneId:$itemIndex") {
    "entry:0" -> R.drawable.guide_entry_door
    "entry:1" -> R.drawable.guide_entry_lock
    "entry:2" -> R.drawable.guide_entry_shoe_cabinet
    "entry:3" -> R.drawable.guide_entry_intercom
    "kitchen:0" -> R.drawable.guide_kitchen_counter
    "kitchen:1" -> R.drawable.guide_kitchen_cabinet_drain
    "kitchen:2" -> R.drawable.guide_kitchen_faucet_drain
    "kitchen:3" -> R.drawable.guide_kitchen_gas
    "window:0" -> R.drawable.guide_window_open_lock
    "window:1" -> R.drawable.guide_window_frame_damage
    "window:2" -> R.drawable.guide_window_mold
    "window:3" -> R.drawable.guide_window_screen
    "room:0" -> R.drawable.guide_room_wall_ceiling
    "room:1" -> R.drawable.guide_room_floor
    "room:2" -> R.drawable.guide_room_outlet
    "room:3" -> R.drawable.guide_room_aircon
    "bathroom:0" -> R.drawable.guide_bath_mold
    "bathroom:1" -> R.drawable.guide_bath_shower
    "bathroom:2" -> R.drawable.guide_bath_drain
    "bathroom:3" -> R.drawable.guide_bath_fan
    else -> R.drawable.guide_entry_door
}
@Composable
private fun GuideStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(PaleGreen), contentAlignment = Alignment.Center) {
            Text(number.toString(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, modifier = Modifier.padding(top = 3.dp), color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
    }
}
