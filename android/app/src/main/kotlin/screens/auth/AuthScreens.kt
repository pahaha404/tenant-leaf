package com.seipseip.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun Login(go: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showUnregisteredDialog by remember { mutableStateOf(false) }
    var openingMetaAi by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(openingMetaAi) {
        if (!openingMetaAi) return@LaunchedEffect
        kotlinx.coroutines.delay(900)
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.facebook.stella"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.facebook.stella"))
        runCatching { context.startActivity(marketIntent) }
            .onFailure { context.startActivity(webIntent) }
        openingMetaAi = false
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.background(PaleGreen, RoundedCornerShape(99.dp)).padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Spa, null, tint = Green, modifier = Modifier.size(17.dp))
            Text("세입세잎", color = Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 5.dp))
        }
        Text("첫 자취를 위한\n안심 점검, 시작해 볼까요?", color = Green, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
        Text("내 매물과 점검 기록을 안전하게 보관하세요.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("로그인", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen)
            Field("아이디 또는 이메일", email, { email = it }, false)
            Field("비밀번호", password, { password = it }, true)
            Text("아이디 찾기   비밀번호 찾기", Modifier.fillMaxWidth(), color = Secondary, fontSize = 10.sp, textAlign = TextAlign.End)
            MainButton("로그인", Orange, enabled = email.isNotBlank() && password.isNotBlank()) { showUnregisteredDialog = true }
            Divider()
            MetaLoginButton(loading = openingMetaAi) { openingMetaAi = true }
            MainButton("게스트 모드로 로그인하기", Color.White, text = Secondary, bordered = true) { go("guest") }
            Text("처음 오셨나요? 회원가입", Modifier.fillMaxWidth().clickable { go("signup") }.padding(8.dp), color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }

    if (showUnregisteredDialog) {
        AlertDialog(
            onDismissRequest = { showUnregisteredDialog = false },
            title = { Text("로그인할 수 없어요", color = DeepGreen, fontWeight = FontWeight.ExtraBold) },
            text = { Text("등록되지 않은 아이디입니다.", color = Secondary) },
            confirmButton = {
                TextButton(onClick = { showUnregisteredDialog = false }) {
                    Text("확인", color = Green, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun MetaLoginButton(loading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Green, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Image(
                painter = painterResource(R.drawable.meta_logo),
                contentDescription = "Meta",
                modifier = Modifier.size(width = 74.dp, height = 28.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("로그인하기", color = Color(0xFF142A35), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SignUp(back: () -> Unit, next: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Color.White)) {
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
                Icon(Icons.Outlined.ArrowBack, "뒤로가기", Modifier.size(19.dp), tint = Green)
            }
            Text(
                "이메일 회원가입",
                modifier = Modifier.weight(1f),
                color = Green,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(40.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("세입세잎을 시작해 볼까요?", color = Green, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("점검 기록과 촬영 근거를 안전하게 보관할 계정을 만들어요.", color = Secondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
            SignUpField("이메일", "example@seipseip.com", email, { email = it }, false)
            SignUpField("비밀번호", "영문·숫자 포함 8자 이상", password, { password = it }, true)
            SignUpField("비밀번호 확인", "비밀번호를 한 번 더 입력해 주세요", passwordConfirm, { passwordConfirm = it }, true)
            SignUpField("닉네임", "앱에서 사용할 이름", nickname, { nickname = it }, false, isNickname = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaleGreen)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ⓘ", color = Green, fontSize = 14.sp)
                Spacer(Modifier.width(7.dp))
                Text("영문과 숫자를 포함해 8자 이상 입력해 주세요.", color = Secondary, fontSize = 10.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 20.dp),
        ) {
            SignUpNextButton { next(nickname.trim().ifBlank { "민지" }) }
        }
    }
}

@Composable
private fun SignUpNextButton(click: () -> Unit) {
    Button(
        onClick = click,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
    ) {
        Text("회원가입 하기", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpField(
    label: String,
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
    password: Boolean,
    isNickname: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = DeepGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            placeholder = { Text(placeholder, color = Color(0xFFA3AAA3), fontSize = 11.sp) },
            leadingIcon = {
                Icon(
                    imageVector = if (password) Icons.Outlined.Key else if (isNickname) Icons.Outlined.Person else Icons.Outlined.Email,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(19.dp),
                )
            },
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Green,
                unfocusedBorderColor = Border,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Green,
            ),
        )
    }
}
