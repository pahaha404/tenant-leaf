package com.seipseip.app.feature.magazine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary

private val MagazineBackground = Color.White
private val MagazineTopBackground = Color(0xFFF6F4EF)
private val MagazineOrangeLight = Color(0xFFFFF0E4)
private val MagazinePurpleLight = Color(0xFFEDE8F8)
private val MagazineSky = Color(0xFF63C7DE)

private data class MagazineArticle(
    val category: String,
    val title: String,
    val description: String,
    val artwork: Int,
)

private val magazineArticles = listOf(
    MagazineArticle("생활 준비", "첫 자취생 필수템\n체크리스트", "이사 첫날부터 필요한 생활 준비물만 골랐어요.", 0),
    MagazineArticle("집 구하기", "집 볼 때 흔히 하는\n5가지 실수", "계약 전 놓치기 쉬운 현장 확인 포인트를 정리했어요.", 1),
    MagazineArticle("계약 전", "계약서 쓰기 전\n반드시 확인할 것", "보증금·관리비·특약을 쉽게 살펴보는 방법이에요.", 2),
    MagazineArticle("생활 관리", "월세방, 오래 편하게\n사는 작은 습관", "곰팡이와 누수를 미리 발견하는 생활 습관이에요.", 3),
)

@Composable
fun MagazineScreen(
    onBack: () -> Unit,
    onOpenArticle: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(MagazineBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MagazineTopBar(title = "자취 매거진", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "처음 자취라면\n꼭 알아둘 생활 정보",
                    color = Green,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "집을 구하고, 계약하고, 살아가는 데 필요한 핵심만 모았어요.",
                    color = Secondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    magazineArticles.forEach { article ->
                        MagazineListItem(article = article, onClick = onOpenArticle)
                    }
                }
            }
        }
    }
}

@Composable
fun MagazineDetailScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MagazineBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MagazineTopBar(title = "자취 매거진", onBack = onBack, showBookmark = true)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 17.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("생활 준비 · 3분 읽기", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "첫 자취,\n뭐부터 챙겨야 할까요?",
                    color = DeepGreen,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "처음 자취를 시작하면 챙길 건 많고, 뭔가 사야 할지는 막막하죠.\n그런데 조금만 준비하면 오늘부터 바로 쓸 수 있는 것들부터, 많이 달라져요.",
                    color = Secondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                BuildingCover()
                Text("가장 먼저 챙길 3가지", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                ArticlePoint(1, "청소 도구", "입주 첫날에는 먼지와 물때부터 신경 써요. 고무장갑, 수세미, 세제, 물걸레 정도면 충분해요.")
                ArticlePoint(2, "욕실 필수품", "슬리퍼와 배수구 망은 생각보다 빠르게 필요해져요. 샤워기필터도 물때 걱정을 조금 덜어줘요.")
                ArticlePoint(3, "생활 안전 도구", "멀티탭, 소화기, 간단한 알림용 보안용품 등은 미리 챙겨두면 언제 갑자기 필요해져도 안심돼요.")
                Text("준비됐나요?", modifier = Modifier.padding(top = 7.dp), color = Green, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("이제 챙길 물건을 하나씩 체크리스트로 준비해 보세요.", color = Secondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MagazineTopBar(title: String, onBack: () -> Unit, showBookmark: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(MagazineBackground),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MagazineTopBackground)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로가기", tint = DeepGreen, modifier = Modifier.size(18.dp))
        }
        Text(title, color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        if (showBookmark) {
            Icon(
                Icons.Outlined.BookmarkBorder,
                contentDescription = "북마크",
                tint = Green,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).size(19.dp),
            )
        }
    }
}

@Composable
private fun MagazineListItem(article: MagazineArticle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MagazineIllustration(article.artwork)
        Spacer(Modifier.width(13.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(article.category, color = Orange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(article.title, color = DeepGreen, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(article.description, color = Secondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun MagazineIllustration(kind: Int) {
    val background = when (kind) {
        0 -> MagazineOrangeLight
        1 -> PaleGreen
        2 -> MagazinePurpleLight
        else -> Color(0xFFFFE8D7)
    }
    // 원본 SVG는 디자인 담당자가 drawable에 추가할 예정입니다.
    Box(
        modifier = Modifier
            .size(width = 112.dp, height = 88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background),
    )
}
@Composable
private fun BuildingCover() {
    // 원본 표지 이미지는 디자인 담당자가 drawable에 추가할 예정입니다.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MagazineSky),
    )
}
@Composable
private fun ArticlePoint(number: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (number == 2) PaleGreen else Color(0xFFFFF8F1))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(description, color = Secondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}