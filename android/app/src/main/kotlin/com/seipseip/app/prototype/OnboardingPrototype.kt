package com.seipseip.app.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

private val Green = Color(0xFF2F6848)
private val DeepGreen = Color(0xFF1E4933)
private val PaleGreen = Color(0xFFEEF4EA)
private val SoftGreen = Color(0xFFDCE9D6)
private val Orange = Color(0xFFF68B38)
private val PaleOrange = Color(0xFFFFF0E4)
private val Secondary = Color(0xFF607267)
private val Border = Color(0xFFD9E1DA)

/** 디자인 브랜치에서 만든 온보딩 시안을 보존한 Preview 전용 화면입니다. */
@Composable fun OnboardingPrototype() = TenantLeafOnboardingPrototype()

@Composable private fun TenantLeafOnboardingPrototype() {
    val nav = rememberNavController()
    MaterialTheme { Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFCFBF8)) {
        NavHost(nav, startDestination = "login") {
            composable("login") { Login { nav.navigate(it) } }
            composable("signup") { SignUp({ nav.popBackStack() }) { nav.navigate("welcome") } }
            composable("welcome") { Welcome({ nav.popBackStack() }) { nav.navigate("consent") } }
            composable("consent") { Consent({ nav.popBackStack() }) { nav.navigate("permissions") } }
            composable("permissions") { Permissions({ nav.popBackStack() }) { nav.navigate("denied") } }
            composable("denied") { Denied({ nav.popBackStack() }) { nav.navigate("complete") } }
            composable("complete") { Complete { nav.navigate("home") { popUpTo("login") { inclusive = true } } } }
            composable("home") { Home() }
        }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Login(go: (String) -> Unit) {
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
            MainButton("로그인",Green,enabled=email.isNotBlank()&&password.isNotBlank()) { go("home") }
            Divider(); MainButton("카카오로 로그인하기",Color(0xFFFEE500),text=Color(0xFF3A2D00)){go("home")}; MainButton("구글로 로그인하기",Color.White,bordered=true){go("home")}; MainButton("메타로 로그인하기",Color(0xFFEEF1FF),text=Color(0xFF3655C9)){go("home")}; MainButton("게스트 모드로 둘러보기",Color.White,text=Secondary,bordered=true){go("home")}
            Text("처음 오셨나요? 회원가입",Modifier.fillMaxWidth().clickable { go("signup") }.padding(8.dp),color=Green,fontWeight=FontWeight.Bold,fontSize=12.sp,textAlign=TextAlign.Center)
        }
    }
}

@Composable private fun SignUp(back:()->Unit,next:()->Unit) { var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; Page("이메일 회원가입",back) {
    Text("세입세잎을 시작해 볼까요?",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("점검 기록과 매물 정보를 안전하게 보관해요.",color=Secondary,fontSize=12.sp); Field("이메일",email,{email=it},false); Field("비밀번호",password,{password=it},true); Field("비밀번호 확인", "", {},true); Tip("입력한 정보는 로그인과 점검 기록 보관에만 사용돼요."); MainButton("다음",Orange){next()}
} }

@Composable private fun Welcome(back:()->Unit,next:()->Unit) = Page("첫 이용 안내",back) {
    Card(colors=CardDefaults.cardColors(containerColor=PaleGreen),shape=RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(26.dp),horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Outlined.Spa,null,tint=Green,modifier=Modifier.size(36.dp)); Text("WELCOME TO 세입세잎",color=Green,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp)) } }
    Text("처음 오셨군요!",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("점검 기록을 남기는 방법을 안내할게요.",color=Secondary,fontSize=12.sp); Notice("1", "매물·방문 일정을 먼저 등록해요",PaleOrange); Notice("2", "카메라 권한을 설정해요",Color.White); Notice("3", "필요한 권한을 확인해요",Color.White); MainButton("필수 안내 확인하기",Orange){next()}
}

@Composable private fun Consent(back:()->Unit,next:()->Unit) { var all by remember { mutableStateOf(false) }; Page("약관 및 개인정보 동의",back) {
    Text("안전한 이용을 위해\n동의가 필요해요",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("필수 약관을 확인해 주세요.",color=Secondary,fontSize=12.sp); CheckRow("전체 동의",all){all=!all}; CheckRow("서비스 이용약관 동의",all){}; CheckRow("개인정보 수집·이용 동의",all){}; CheckRow("만 14세 이상 확인",all){}; Tip("필수 약관에 동의해야 서비스를 이용할 수 있어요."); MainButton("동의하고 계속하기",Orange){next()}
} }

@Composable private fun Permissions(back:()->Unit,next:()->Unit) = Page("권한 설정",back) {
    Text("점검에 필요한 권한을\n확인해 주세요",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("필수 권한은 점검 사진과 기록을 위해 사용돼요.",color=Secondary,fontSize=12.sp); Permission("카메라", "점검 사진을 촬영해요", "필수"); Permission("마이크", "영상과 음성 메모를 남겨요", "선택"); Permission("블루투스", "세입세잎 Glass를 연결해요", "선택"); Permission("알림", "분석 완료 소식을 알려드려요", "선택"); Tip("권한은 휴대폰 설정에서 언제든 바꿀 수 있어요."); MainButton("권한 설정 계속하기",Orange){next()}
}

@Composable private fun Denied(back:()->Unit,next:()->Unit) = Page("권한 거부 안내",back) {
    Text("필수 권한이 꺼져 있어요",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("카메라 권한이 없으면 점검 사진을 촬영할 수 없어요.",color=Secondary,fontSize=12.sp); Card(colors=CardDefaults.cardColors(containerColor=PaleOrange),shape=RoundedCornerShape(16.dp)) { Text("설정에서 카메라 권한을 허용해 주세요.",Modifier.fillMaxWidth().padding(22.dp),color=DeepGreen,textAlign=TextAlign.Center,fontWeight=FontWeight.Bold) }; Permission("카메라", "현재: 허용 안 함", "필수"); Permission("마이크", "현재: 허용 안 함", "선택"); MainButton("설정에서 권한 허용",Orange){next()}
}

@Composable private fun Complete(next:()->Unit) { Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Card(colors=CardDefaults.cardColors(containerColor=Green),shape=RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Outlined.Spa,null,tint=Color.White,modifier=Modifier.size(38.dp)); Text("로그인 완료",color=Color.White,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(top=10.dp)); Text("세입세잎",color=SoftGreen,fontSize=12.sp) } }; Spacer(Modifier.height(30.dp)); Text("다시 만나서 반가워요",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Green); Text("이제 내 매물과 점검 기록을 관리할 수 있어요.",color=Secondary,fontSize=12.sp,modifier=Modifier.padding(top=8.dp)); Spacer(Modifier.height(24.dp)); Tip("점검 기록은 내 계정에 안전하게 보관돼요."); Spacer(Modifier.height(24.dp)); MainButton("홈으로 돌아가기",Orange){next()} } }

@Composable private fun Home() = Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Outlined.Spa,null,tint=Green,modifier=Modifier.size(42.dp)); Text("세입세잎 홈",color=Green,fontSize=25.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(top=12.dp)); Text("온보딩을 완료했어요.",color=Secondary,fontSize=13.sp) }

@Composable private fun Page(title:String,back:()->Unit,content:@Composable ColumnScope.()->Unit) = Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,"뒤로",Modifier.clickable { back() },tint=Green); Text(title,Modifier.padding(start=12.dp),color=Green,fontWeight=FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); content() }
@OptIn(ExperimentalMaterial3Api::class) @Composable private fun Field(label:String,value:String,onChange:(String)->Unit,password:Boolean) = OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label)},leadingIcon={Icon(if(password) Icons.Outlined.Key else Icons.Outlined.Email,null)},visualTransformation=if(password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,singleLine=true,shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Green,focusedLabelColor=Green,unfocusedBorderColor=Border))
@Composable private fun MainButton(label:String,color:Color,text:Color=Color.White,enabled:Boolean=true,bordered:Boolean=false,click:()->Unit) = Button(click,Modifier.fillMaxWidth().height(50.dp),enabled=enabled,shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.buttonColors(containerColor=color,contentColor=text),border=if(bordered) androidx.compose.foundation.BorderStroke(1.dp,Border) else null){Text(label,fontWeight=FontWeight.Bold,fontSize=12.sp)}
@Composable private fun Divider() = Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f).height(1.dp).background(Border));Text("또는",Modifier.padding(horizontal=10.dp),color=Secondary,fontSize=10.sp);Box(Modifier.weight(1f).height(1.dp).background(Border))}
@Composable private fun Tip(text:String)=Card(colors=CardDefaults.cardColors(containerColor=PaleGreen),shape=RoundedCornerShape(12.dp)){Text(text,Modifier.padding(12.dp),color=Secondary,fontSize=11.sp)}
@Composable private fun Notice(number:String,text:String,color:Color)=Card(colors=CardDefaults.cardColors(containerColor=color),shape=RoundedCornerShape(10.dp)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(number,color=Orange,fontWeight=FontWeight.Bold);Text(text,Modifier.padding(start=10.dp),fontSize=12.sp,color=DeepGreen)}}
@Composable private fun CheckRow(text:String,checked:Boolean,click:()->Unit)=Card(Modifier.fillMaxWidth().clickable{click()},colors=CardDefaults.cardColors(containerColor=if(checked)PaleGreen else Color.White),shape=RoundedCornerShape(10.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Text(if(checked)"●" else "○",color=Green);Text(text,Modifier.padding(start=10.dp),fontSize=12.sp,color=DeepGreen);Spacer(Modifier.weight(1f));Text("필수",color=Orange,fontSize=10.sp)}}
@Composable private fun Permission(title:String,detail:String,badge:String)=Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(12.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Spa,null,tint=Green);Column(Modifier.padding(start=10.dp).weight(1f)){Text(title,fontSize=12.sp,fontWeight=FontWeight.Bold,color=DeepGreen);Text(detail,fontSize=10.sp,color=Secondary)};Text(badge,color=if(badge=="필수")Orange else Green,fontSize=10.sp,fontWeight=FontWeight.Bold)}}
@Preview(showBackground=true,widthDp=390,heightDp=844) @Composable private fun Preview()=TenantLeafOnboardingPrototype()
