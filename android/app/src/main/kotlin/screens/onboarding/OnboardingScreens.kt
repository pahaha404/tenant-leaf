package com.seipseip.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.Brush
import com.seipseip.app.PageWithBottomAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private data class OnboardingSlide(
    @param:DrawableRes val imageRes: Int,
    val contentDescription: String,
)

@Composable
internal fun Welcome(
    back: () -> Unit = {},
    next: () -> Unit,
) {
    val slides = remember {
        listOf(
            OnboardingSlide(
                imageRes = R.drawable.onboarding_1,
                contentDescription = "매물 비교와 하자 정보 과부하 안내 화면",
            ),
            OnboardingSlide(
                imageRes = R.drawable.onboarding_2,
                contentDescription = "스마트 글라스와 AI, AR 기술을 활용한 하자 점검 안내 화면",
            ),
            OnboardingSlide(
                imageRes = R.drawable.onboarding_3,
                contentDescription = "세입세잎과 함께하는 안심 집 찾기 서비스 시작 화면",
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val isLastPage by remember { derivedStateOf { pagerState.currentPage == slides.lastIndex } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val slide = slides[index]
            Image(
                painter = painterResource(id = slide.imageRes),
                contentDescription = slide.contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // 하단 컨트롤 가독성을 위한 부드러운 그라디언트 스크림
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OnboardingPagerIndicator(
                    pageCount = slides.size,
                    currentPage = pagerState.currentPage,
                )

                AnimatedVisibility(
                    visible = isLastPage,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        MainButton(
                            label = "시작하기",
                            color = Green,
                            click = next,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 6.dp,
                label = "indicatorWidth_$index",
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .background(
                        color = if (isSelected) Green else Color.White.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(99.dp),
                    ),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomePreview() {
    Welcome(next = {})
}

@Composable
internal fun FirstUse(back: () -> Unit, next: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = back,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(99.dp)),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로가기", tint = Green)
            }
            Text(
                "첫 이용 안내",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Green,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(30.dp))
            Card(
                modifier = Modifier.fillMaxWidth().height(190.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PaleGreen),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(Color.White, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Spa, null, tint = Green, modifier = Modifier.size(31.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("WELCOME TO SEIPSEIP", color = Green, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("처음 오셨군요!", color = Green, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "필수 동의와 권한 설정을 마치면 튜토리얼을 보고 세입세잎을 시작할 수 있어요.",
                color = Secondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(16.dp))
            FirstUseStep(1, "약관·개인정보 동의", active = true)
            Spacer(Modifier.height(8.dp))
            FirstUseStep(2, "카메라·마이크·블루투스 권한", active = false)
            Spacer(Modifier.height(8.dp))
            FirstUseStep(3, "튜토리얼과 안심 가이드", active = false)
            Spacer(Modifier.height(12.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            MainButton("필수 동의 확인하기", Orange, click = next)
        }
    }
}

@Composable
private fun FirstUseStep(number: Int, label: String, active: Boolean) {
    val container = if (active) Color(0xFFFFF0E4) else Color.White
    val border = if (active) Color(0xFFF2C69F) else Color(0xFFD9E1DA)
    val badge = if (active) Orange else Color(0xFFDCE9D6)
    Card(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(26.dp).background(badge, RoundedCornerShape(99.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(number.toString(), color = if (active) Color.White else Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(9.dp))
            Text(label, color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
@Composable internal fun Consent(back: () -> Unit, next: () -> Unit) {
    var termsAgreed by remember { mutableStateOf(false) }
    var privacyAgreed by remember { mutableStateOf(false) }
    var ageConfirmed by remember { mutableStateOf(false) }
    val allAgreed = termsAgreed && privacyAgreed && ageConfirmed

    PageWithBottomAction("약관 및 개인정보 동의", back, action = {
        MainButton("동의하고 계속하기", Orange, enabled = allAgreed) { next() }
    }) {
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

    }
}

@Composable
internal fun Permissions(
    back: () -> Unit,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    val context = LocalContext.current
    val permissions = remember { tenantLeafRuntimePermissions() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initializeDatWhenPermitted(context)
            onGranted()
        } else {
            onDenied()
        }
    }
    fun requestMissingPermissions() {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            initializeDatWhenPermitted(context)
            onGranted()
        } else {
            launcher.launch(missing.toTypedArray())
        }
    }
    LaunchedEffect(Unit) { requestMissingPermissions() }
    PageWithBottomAction("권한 설정", back, action = {
        MainButton("권한 설정 계속하기", Orange, click = ::requestMissingPermissions)
    }) {
        Text("점검에 필요한 권한을\n확인해 주세요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Green)
        Text("필수 권한은 점검 사진과 기록을 위해 사용돼요.", color = Secondary, fontSize = 12.sp)
        Permission("카메라", "점검 사진을 촬영해요", "필수")
        Permission("마이크", "영상과 음성 메모를 남겨요", "선택")
        Permission("블루투스", "세입세잎 Glass를 연결해요", "선택")
        Permission("알림", "분석 완료 소식을 알려드려요", "선택")
        Tip("권한은 휴대폰 설정에서 언제든 바꿀 수 있어요.")

    }
}

@Composable
internal fun Denied(back: () -> Unit) = PageWithBottomAction("권한 거부 안내", back, action = {
    val context = LocalContext.current
    MainButton("휴대폰 설정 열기", Orange) {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
}) {
    Text("필수 권한이 꺼져 있어요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Green)
    Text("카메라 권한이 없으면 점검 사진을 촬영할 수 없어요.", color = Secondary, fontSize = 12.sp)
    Card(colors = CardDefaults.cardColors(containerColor = PaleOrange), shape = RoundedCornerShape(16.dp)) {
        Text("설정에서 카메라 권한을 허용해 주세요.", Modifier.fillMaxWidth().padding(22.dp), color = DeepGreen, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    }
    Permission("카메라", "현재: 허용 안 함", "필수")
    Permission("마이크", "현재: 허용 안 함", "선택")
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
@Composable internal fun Complete(next:()->Unit) { Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) { Column(Modifier.weight(1f).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) { Card(colors=CardDefaults.cardColors(containerColor=Green),shape=RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Outlined.Spa,null,tint=Color.White,modifier=Modifier.size(38.dp)); Text("로그인 완료",color=Color.White,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(top=10.dp)); Text("세입세잎",color=SoftGreen,fontSize=12.sp) } }; Spacer(Modifier.height(30.dp)); Text("다시 만나서 반가워요",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("이제 내 매물과 점검 기록을 관리할 수 있어요.",color=Secondary,fontSize=12.sp,modifier=Modifier.padding(top = 8.dp)); Spacer(Modifier.height(24.dp)); Tip("점검 기록은 내 계정에 안전하게 보관돼요.") }; MainButton("홈으로 돌아가기",Orange){next()} } }
