package com.seipseip.app.feature.magazine

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.R
import com.seipseip.app.Secondary

private val MagazineBackground = Color.White
private val MagazineTopBackground = Color(0xFFF6F4EF)

private data class ArticlePointData(val title: String, val description: String)

private data class MagazineArticle(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val imageRes: Int,
    val readTime: String,
    val intro: String,
    val sectionTitle: String,
    val points: List<ArticlePointData>,
    val closingTitle: String,
    val closingDescription: String,
)

private val magazineArticles = listOf(
    MagazineArticle(
        "first_essentials", "생활 준비", "첫 자취생 필수템\n체크리스트", "이사 첫날부터 필요한 생활 준비물", R.drawable.magazine_1, "3분 읽기",
        "처음 자취를 시작하면 필요한 물건이 끝없이 떠오르죠. 한 번에 다 사기보다 입주 첫날 바로 쓰는 것, 일주일 안에 필요한 것, 생활하면서 고르는 것으로 나누면 낭비를 줄일 수 있어요.",
        "입주 첫날, 먼저 챙길 3가지",
        listOf(
            ArticlePointData("청소 도구", "고무장갑, 수세미, 다목적 세제, 물걸레는 입주 직후 청소에 바로 필요해요. 새집이라도 싱크대·욕실·창틀은 한 번 닦아보세요."),
            ArticlePointData("잠자리와 욕실", "침구, 수건, 휴지, 슬리퍼처럼 첫날 밤과 다음 날 아침에 쓸 물건을 우선 챙기세요. 배수구 망도 함께 준비하면 편해요."),
            ArticlePointData("전기와 안전", "멀티탭은 정격 용량을 확인하고, 작은 손전등·상비약·소화기 위치 확인까지 해두면 갑작스러운 상황에 덜 당황해요."),
        ),
        "필수템은 생활하며 채워도 돼요", "자취 초기에 가장 흔한 실수는 혹시 필요할지 몰라서 물건을 한꺼번에 사두는 거예요. 먼저 잠자리·욕실·청소처럼 오늘 바로 쓸 물건부터 준비해 보세요. 일주일을 살아보면 수납이 부족한 곳, 조리할 때 불편한 동선, 자주 쓰는 물건의 자리가 자연스럽게 보입니다. 그때 치수와 예산을 적어 두고 필요한 용품을 고르면, 작은 방도 훨씬 덜 어수선해져요. 에디터는 첫 장보기 예산의 일부를 남겨 두었다가 실제 생활에 맞는 물건을 고르는 방법을 추천해요.",
    ),
    MagazineArticle(
        "home_viewing_mistakes", "집 구하기", "집 볼 때 흔히 하는\n5가지 실수", "계약 전 놓치기 쉬운 현장 확인 포인트", R.drawable.magazine_2, "4분 읽기",
        "마음에 드는 집을 만나면 채광이나 인테리어부터 보게 되지만, 실제 생활은 물·소리·환기처럼 사진에 잘 안 보이는 부분에서 결정돼요. 짧은 방문에서도 확인할 순서를 정해두세요.",
        "현장에서 놓치기 쉬운 5가지",
        listOf(
            ArticlePointData("낮과 밤의 환경", "가능하면 낮에 채광을 보고, 주변 공사장·도로·상가 소음은 창문을 열고 들어보세요. 밤 귀가 동선도 지도와 직접 확인해요."),
            ArticlePointData("물과 배수", "싱크대와 샤워기를 직접 틀어 수압·온수·배수 속도를 확인하세요. 하부장과 세면대 아래 물자국도 꼭 보세요."),
            ArticlePointData("창문과 환기", "창문을 끝까지 열고 닫아 잠금이 되는지 확인하세요. 창틀 결로·곰팡이 흔적, 방충망 상태도 함께 봐야 해요."),
            ArticlePointData("수납과 가구 배치", "사진상 넓어 보여도 침대·책상·옷장이 들어가면 동선이 달라져요. 줄자로 벽 길이와 문 열리는 방향을 재두면 좋아요."),
            ArticlePointData("관리비와 공용 공간", "관리비에 포함되는 항목, 쓰레기 배출 장소, 주차·엘리베이터·공용 현관 상태를 확인하면 입주 뒤의 불편을 줄일 수 있어요."),
        ),
        "좋아 보이는 집보다, 확인한 집", "집을 여러 곳 보면 처음에는 채광이나 인테리어처럼 눈에 잘 들어오는 장점만 기억하기 쉬워요. 그래서 방문 전에는 꼭 확인할 항목을 정하고, 현장에서는 같은 순서로 문을 열고 물을 틀어보는 것이 좋습니다. 마음에 드는 집일수록 더 천천히 확인해야 한다는 점도 기억하세요. 작은 물자국, 창문 잠금의 헐거움, 예상보다 큰 소음은 짧은 방문에서 놓치기 쉽습니다. 사진과 메모를 남겨 두면 감상 대신 기록으로 비교할 수 있고, 계약 직전에도 다시 질문할 근거가 생깁니다.",
    ),
    MagazineArticle(
        "contract_checklist", "계약 전", "계약서 쓰기 전\n반드시 확인할 것", "보증금·관리비·특약, 쉽게 살펴봐요", R.drawable.magazine_3, "4분 읽기",
        "계약서는 집의 상태와 약속을 남기는 문서예요. 서명 전에 이해되지 않는 항목은 넘기지 말고, 보증금·관리비·수리 책임처럼 생활에 영향을 주는 내용을 문장으로 확인해야 합니다.",
        "계약서에서 확인할 핵심",
        listOf(
            ArticlePointData("계약 당사자와 주소", "등기부등본의 소유자와 계약 상대방이 같은지 확인하고, 동·호수와 전용면적이 실제 본 집과 맞는지 살펴보세요."),
            ArticlePointData("보증금과 월세 지급일", "보증금·월세·계약 기간·입금 계좌를 다시 확인하세요. 계약금과 잔금의 날짜, 영수증 또는 이체 기록도 보관해요."),
            ArticlePointData("관리비의 포함 항목", "관리비 금액만 보지 말고 수도·인터넷·공용전기·주차비 중 무엇이 포함되는지 물어보세요. 계절별 추가 비용도 확인하면 좋아요."),
            ArticlePointData("수리와 특약", "입주 전 발견한 하자는 사진으로 남기고, 수리 주체와 기한을 특약에 적어두세요. 구두 약속은 나중에 확인하기 어렵습니다."),
        ),
        "모르면 서명 전에 질문하세요", "계약서는 빨리 서명하는 사람이 아니라, 내용을 끝까지 이해한 사람이 더 안전하게 마무리할 수 있는 문서예요. 관리비에 무엇이 포함되는지, 고장 났을 때 누가 언제 수리하는지처럼 생활에 바로 영향을 주는 약속은 특히 구체적으로 확인하세요. 구두로 들은 내용은 메모로 남기고, 가능하면 특약이나 문자처럼 다시 확인할 수 있는 형태로 정리하는 편이 좋습니다. 설명을 들어도 모호한 항목이 있다면 서명을 미루고 질문해도 됩니다. 필요할 때는 공인중개사·지자체 상담 창구 등 신뢰할 수 있는 곳에 한 번 더 확인해 보세요.",
    ),
    MagazineArticle(
        "room_care_habits", "생활 관리", "월세방, 오래 편하게\n사는 작은 습관", "곰팡이와 누수를 미리 발견하는 생활 습관", R.drawable.magazine_1, "3분 읽기",
        "작은 월세방도 환기와 물기 관리만 꾸준히 하면 훨씬 쾌적하게 유지할 수 있어요. 문제가 커진 뒤보다 처음 보였을 때 사진과 날짜를 남기는 습관이 중요합니다.",
        "주 10분으로 하는 방 관리",
        listOf(
            ArticlePointData("샤워 후 물기 닦기", "욕실 벽과 바닥의 큰 물기를 밀대로 정리하고 문을 열어두세요. 실리콘과 배수구 주변은 주기적으로 확인해요."),
            ArticlePointData("창문은 짧고 자주", "요리·샤워 뒤 5~10분씩 맞바람이 나게 환기하면 결로를 줄일 수 있어요. 비 오는 날은 제습기나 환풍기를 함께 써요."),
            ArticlePointData("싱크대 아래 확인", "일주일에 한 번 하부장을 열어 냄새와 물기를 보세요. 휴지로 배수관 주변을 닦아보면 작은 누수도 빨리 알 수 있어요."),
        ),
        "기록이 가장 쉬운 예방이에요", "월세방 관리는 큰 청소를 한 번 하는 것보다, 작은 이상을 초기에 발견하는 습관에서 시작돼요. 욕실 실리콘의 검은 점, 창틀의 물방울, 싱크대 아래의 냄새처럼 사소해 보여도 날짜와 위치를 남겨 두세요. 같은 자리에 반복되는 변색이나 물기는 원인을 확인해야 한다는 신호일 수 있습니다. 사진은 밝은 곳에서 넓게 한 장, 문제 부위를 가까이 한 장 찍어두면 설명할 때 도움이 돼요. 집주인이나 관리인에게는 감정적인 표현보다 발견한 날짜와 상태를 차분히 전달하는 것이 문제 해결을 앞당깁니다.",
    ),
)

private fun magazineArticle(id: String) = magazineArticles.firstOrNull { it.id == id } ?: magazineArticles.first()

@Composable
fun MagazineScreen(onBack: () -> Unit, onOpenArticle: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MagazineBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MagazineTopBar(title = "자취 매거진", onBack = onBack)
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("처음 자취라면\n꼭 알아둘 생활 정보", color = Green, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("집을 구하고, 계약하고, 살아가는 데 필요한 핵심만 모았어요.", color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
                Column(modifier = Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    magazineArticles.forEach { article -> MagazineListItem(article) { onOpenArticle(article.id) } }
                }
            }
        }
    }
}

@Composable
fun MagazineDetailScreen(articleId: String, onBack: () -> Unit) {
    val article = magazineArticle(articleId)
    Box(modifier = Modifier.fillMaxSize().background(MagazineBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MagazineTopBar(title = "자취 매거진", onBack = onBack, showBookmark = true)
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 17.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${article.category} · ${article.readTime}", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(article.title, color = DeepGreen, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(article.description, color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
                MagazineCover(article.imageRes, article.title.replace("\n", " "))
                Text(article.intro, color = Secondary, fontSize = 12.sp, lineHeight = 19.sp)
                Text(article.sectionTitle, color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                article.points.forEachIndexed { index, point -> ArticlePoint(index + 1, point.title, point.description) }
                Text(article.closingTitle, modifier = Modifier.padding(top = 7.dp), color = Green, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(article.closingDescription, color = Secondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun MagazineTopBar(title: String, onBack: () -> Unit, showBookmark: Boolean = false) {
    Box(modifier = Modifier.fillMaxWidth().height(54.dp).background(MagazineBackground), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp).size(32.dp).clip(CircleShape).background(MagazineTopBackground).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로가기", tint = DeepGreen, modifier = Modifier.size(18.dp))
        }
        Text(title, color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        if (showBookmark) Icon(Icons.Outlined.BookmarkBorder, contentDescription = "북마크", tint = Green, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).size(19.dp))
    }
}

@Composable
private fun MagazineListItem(article: MagazineArticle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF8F1))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painter = painterResource(article.imageRes), contentDescription = "${article.title.replace("\n", " ")} 이미지", modifier = Modifier.size(width = 112.dp, height = 88.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(article.category, color = Orange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(article.title, color = DeepGreen, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(article.description, color = Secondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun MagazineCover(imageRes: Int, description: String) {
    Image(painter = painterResource(imageRes), contentDescription = "$description 표지 이미지", modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
}

@Composable
private fun ArticlePoint(number: Int, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (number % 2 == 0) PaleGreen else Color(0xFFFFF8F1)).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Text(number.toString(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(description, color = Secondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}