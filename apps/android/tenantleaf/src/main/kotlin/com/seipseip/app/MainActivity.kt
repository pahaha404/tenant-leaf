package com.seipseip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ForestGreen = Color(0xFF2F6848)
private val DarkForest = Color(0xFF1E4933)
private val SagePale = Color(0xFFEEF4EA)
private val SageSoft = Color(0xFFDCE9D6)
private val WarmOrange = Color(0xFFF28A3A)
private val TextSecondary = Color(0xFF607267)
private val Line = Color(0xFFD9E1DA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SeipseipApp() }
    }
}

@Composable
private fun SeipseipApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            AssuranceLoginScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssuranceLoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var lastAction by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkForest)
                .padding(start = 24.dp, end = 24.dp, top = 54.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Spa, null, tint = SageSoft, modifier = Modifier.size(18.dp))
                Text("세입세잎", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
            }
            Spacer(Modifier.height(30.dp))
            Text("내 점검 기록을\n안전하게 이어가세요", color = Color.White, fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("한 번의 로그인으로 사진·메모·리포트를 모두 관리해요.", color = SageSoft, fontSize = 12.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("간편하게 시작하기", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = DarkForest)
            Text("자주 쓰는 계정으로 로그인해 주세요.", fontSize = 12.sp, color = TextSecondary)

            SocialLoginButton("카카오로 3초 만에 시작하기", Color(0xFFFEE500), Color(0xFF3A2D00), Icons.Outlined.Message) { lastAction = "카카오 로그인" }
            SocialLoginButton("네이버로 계속하기", ForestGreen, Color.White, Icons.Outlined.Nfc) { lastAction = "네이버 로그인" }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(Line))
                Text("또는", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp))
                Box(Modifier.weight(1f).height(1.dp).background(Line))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("이메일") },
                leadingIcon = { Icon(Icons.Outlined.Email, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("비밀번호") },
                leadingIcon = { Icon(Icons.Outlined.Key, null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors()
            )

            Button(
                onClick = { lastAction = "이메일 로그인" },
                enabled = email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, disabledContainerColor = SageSoft)
            ) { Text("로그인", fontWeight = FontWeight.Bold) }

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SagePale).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Shield, null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                Text("점검 기록은 내 계정에 안전하게 보관돼요.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
            }

            lastAction?.let { Text("임시 UI 상태: $it", color = WarmOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Text("계속하면 이용약관 및 개인정보 처리방침에 동의하게 돼요.", modifier = Modifier.fillMaxWidth(), color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SocialLoginButton(label: String, background: Color, content: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = content)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ForestGreen,
    focusedLabelColor = ForestGreen,
    unfocusedBorderColor = Line,
    focusedLeadingIconColor = ForestGreen,
    unfocusedLeadingIconColor = TextSecondary
)

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun AssuranceLoginPreview() = SeipseipApp()
