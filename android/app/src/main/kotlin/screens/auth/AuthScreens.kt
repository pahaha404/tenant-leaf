package com.seipseip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun Login(go: (String) -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().height(200.dp).background(DeepGreen).statusBarsPadding().padding(24.dp,18.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Outlined.Spa,null,tint=SoftGreen); Text("세입세잎",color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=8.dp)) }
            Spacer(Modifier.height(24.dp)); Text("첫 자취를 위한\n안심 점검, 시작해 볼까요?",color=Color.White,fontSize=25.sp,lineHeight=31.sp,fontWeight=FontWeight.ExtraBold); Spacer(Modifier.height(6.dp)); Text("내 매물과 점검 기록을 안전하게 보관하세요.",color=SoftGreen,fontSize=12.sp)
        }
        Column(Modifier.padding(24.dp,22.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("로그인",fontSize=19.sp,fontWeight=FontWeight.ExtraBold,color=DeepGreen)
            Field("아이디 또는 이메일",email,{email=it},false); Field("비밀번호",password,{password=it},true)
            Text("아이디 찾기   비밀번호 찾기",Modifier.fillMaxWidth(),color=Secondary,fontSize=10.sp,textAlign=TextAlign.End)
            MainButton("로그인",Green,enabled=email.isNotBlank()&&password.isNotBlank()) { go("login") }
            Divider(); MainButton("카카오로 로그인하기",Color(0xFFFEE500),text=Color(0xFF3A2D00)){go("login")}; MainButton("구글로 로그인하기",Color.White,bordered=true){go("login")}; MainButton("메타로 로그인하기",Color(0xFFEEF1FF),text=Color(0xFF3655C9)){go("login")}; MainButton("게스트 모드로 둘러보기",Color.White,text=Secondary,bordered=true){go("guest")}
            Text("처음 오셨나요? 회원가입",Modifier.fillMaxWidth().clickable { go("signup") }.padding(8.dp),color=Green,fontWeight=FontWeight.Bold,fontSize=12.sp,textAlign=TextAlign.Center)
        }
    }
}

@Composable internal fun SignUp(back:()->Unit,next:()->Unit) { var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; Page("이메일 회원가입",back) {
    Text("세입세잎을 시작해 볼까요?",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("점검 기록과 매물 정보를 안전하게 보관해요.",color=Secondary,fontSize=12.sp); Field("이메일",email,{email=it},false); Field("비밀번호",password,{password=it},true); Field("비밀번호 확인", "", {},true); Tip("입력한 정보는 로그인과 점검 기록 보관에만 사용돼요."); MainButton("다음",Orange){next()}
} }

