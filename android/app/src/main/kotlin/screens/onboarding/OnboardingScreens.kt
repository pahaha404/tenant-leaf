package com.seipseip.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
internal fun Welcome(back: () -> Unit, next: () -> Unit) {
    val slides = listOf(
        "내 눈으로 확인하고,\n증거로 안심해요" to "스마트 글라스와 함께 방을 둘러보면\n체크리스트·사진·AI 관찰을 차곡차곡 기록해 드려요.",
        "찍고, 듣고,\n놓치지 않아요" to "중요한 순간은 사진으로 남기고,\n음성 안내를 따라 차근차근 점검해요.",
        "기록이 모이면,\n안심이 남아요" to "점검 결과를 한눈에 보는 리포트로 정리해\n계약 전에도, 이사 후에도 든든하게.",
    )
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF6F4EF)).padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.background(PaleGreen, RoundedCornerShape(99.dp)).padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Spa, null, tint = Green, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("세입세잎", color = Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.weight(1f))
            Text("${page + 1} / ${slides.size}", color = Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { index ->
            val (title, description) = slides[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(42.dp))
                Box(
                    modifier = Modifier.size(270.dp).background(PaleGreen, RoundedCornerShape(135.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    // UI.pen의 생성 캐릭터 이미지를 drawable로 추가하면 이 자리에 연결합니다.
                    Icon(Icons.Outlined.Spa, null, tint = Green, modifier = Modifier.size(72.dp))
                }
                Spacer(Modifier.height(34.dp))
                Text(title, modifier = Modifier.fillMaxWidth(), color = Green, fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(11.dp))
                Text(description, modifier = Modifier.fillMaxWidth(), color = Secondary, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(slides.size) { index ->
                Box(
                    modifier = Modifier.height(7.dp).width(if (index == page) 24.dp else 7.dp).background(if (index == page) Green else Color(0xFFD9E1DA), RoundedCornerShape(99.dp)),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        MainButton(if (page == slides.lastIndex) "서비스 시작하기" else "다음", Orange) {
            if (page == slides.lastIndex) next() else scope.launch { pagerState.animateScrollToPage(page + 1) }
        }
    }
}
@Composable internal fun Consent(back: () -> Unit, next: () -> Unit) {
    var termsAgreed by remember { mutableStateOf(false) }
    var privacyAgreed by remember { mutableStateOf(false) }
    var ageConfirmed by remember { mutableStateOf(false) }
    val allAgreed = termsAgreed && privacyAgreed && ageConfirmed

    Page("약관 및 개인정보 동의", back) {
        Text(
            "안전한 이용을 위해\n동의가 필요해요",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Green,
        )
        Text("필수 약관을 확인해 주세요.", color = Secondary, fontSize = 12.sp)

        CheckRow("전체 동의", allAgreed) {
            val nextValue = !allAgreed
            termsAgreed = nextValue
            privacyAgreed = nextValue
            ageConfirmed = nextValue
        }
        CheckRow("서비스 이용약관 동의", termsAgreed) {
            termsAgreed = !termsAgreed
        }
        CheckRow("개인정보 수집·이용 동의", privacyAgreed) {
            privacyAgreed = !privacyAgreed
        }
        CheckRow("만 14세 이상 확인", ageConfirmed) {
            ageConfirmed = !ageConfirmed
        }

        Tip("필수 약관에 모두 동의해야 다음 단계로 갈 수 있어요.")
        MainButton(
            "동의하고 계속하기",
            Orange,
            enabled = allAgreed,
        ) {
            next()
        }
    }
}

@Composable
internal fun Permissions(
    back: () -> Unit,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.CAMERA] == true) onGranted() else onDenied()
    }
    Page("권한 설정", back) {
        Text("점검에 필요한 권한을\n확인해 주세요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Green)
        Text("필수 권한은 점검 사진과 기록을 위해 사용돼요.", color = Secondary, fontSize = 12.sp)
        Permission("카메라", "점검 사진을 촬영해요", "필수")
        Permission("마이크", "영상과 음성 메모를 남겨요", "선택")
        Permission("블루투스", "세입세잎 Glass를 연결해요", "선택")
        Permission("알림", "분석 완료 소식을 알려드려요", "선택")
        Tip("권한은 휴대폰 설정에서 언제든 바꿀 수 있어요.")
        MainButton("권한 설정 계속하기", Orange) { launcher.launch(tenantLeafRuntimePermissions()) }
    }
}

@Composable
internal fun Denied(back: () -> Unit) = Page("권한 거부 안내", back) {
    val context = LocalContext.current
    Text("필수 권한이 꺼져 있어요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Green)
    Text("카메라 권한이 없으면 점검 사진을 촬영할 수 없어요.", color = Secondary, fontSize = 12.sp)
    Card(colors = CardDefaults.cardColors(containerColor = PaleOrange), shape = RoundedCornerShape(16.dp)) {
        Text("설정에서 카메라 권한을 허용해 주세요.", Modifier.fillMaxWidth().padding(22.dp), color = DeepGreen, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    }
    Permission("카메라", "현재: 허용 안 함", "필수")
    Permission("마이크", "현재: 허용 안 함", "선택")
    MainButton("휴대폰 설정 열기", Orange) {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }
}
private fun tenantLeafRuntimePermissions(): Array<String> = buildList {
    add(Manifest.permission.CAMERA)
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()
@Composable internal fun Complete(next:()->Unit) { Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Card(colors=CardDefaults.cardColors(containerColor=Green),shape=RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Outlined.Spa,null,tint=Color.White,modifier=Modifier.size(38.dp)); Text("로그인 완료",color=Color.White,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(top=10.dp)); Text("세입세잎",color=SoftGreen,fontSize=12.sp) } }; Spacer(Modifier.height(30.dp)); Text("다시 만나서 반가워요",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("이제 내 매물과 점검 기록을 관리할 수 있어요.",color=Secondary,fontSize=12.sp,modifier=Modifier.padding(top = 8.dp)); Spacer(Modifier.height(24.dp)); Tip("점검 기록은 내 계정에 안전하게 보관돼요."); Spacer(Modifier.height(24.dp)); MainButton("홈으로 돌아가기",Orange){next()} } }

