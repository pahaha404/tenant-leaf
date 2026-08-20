package com.seipseip.app.feature.property.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.DeepGreen
import com.seipseip.app.Secondary
import com.seipseip.app.feature.common.AppPageScaffold
import com.seipseip.app.feature.common.PrimaryButton

@Composable
fun AddressPickerScreen(onBack: () -> Unit, onConfirmed: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<AddressCandidate>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val query = normalizeAddressQuery(input)

    LaunchedEffect(query) {
        results = emptyList()
        error = null
        if (query == null) return@LaunchedEffect
        delay(350)
        loading = true
        results = runCatching { KakaoAddressSearch.search(query) }
            .onFailure { error = "주소 검색을 완료하지 못했습니다." }
            .getOrDefault(emptyList())
        loading = false
    }

    AppPageScaffold(title = "주소 선택", onBack = onBack) {
        Text("점검할 집의 주소를 입력하세요", color = DeepGreen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("두 글자 이상 입력하면 카카오 주소 결과가 표시됩니다.", color = Secondary, fontSize = 13.sp)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("주소") },
                supportingText = if (input.isNotBlank() && query == null) {
                    { Text("두 글자 이상 입력하세요.") }
                } else {
                    null
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DeepGreen,
                    unfocusedTextColor = DeepGreen,
                    cursorColor = DeepGreen,
                    focusedBorderColor = DeepGreen,
                    focusedLabelColor = DeepGreen,
                ),
            )
            if (loading) Text("주소를 찾는 중입니다.", color = Secondary, fontSize = 13.sp)
            error?.let { Text(it, color = Secondary, fontSize = 13.sp) }
            results.forEach { candidate ->
                Column(
                    modifier = Modifier.fillMaxWidth().clickable { onConfirmed(candidate.address) }.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(candidate.address, color = DeepGreen, fontWeight = FontWeight.Bold)
                    if (candidate.detail != candidate.address) Text(candidate.detail, color = Secondary, fontSize = 12.sp)
                }
            }
        }
    }
}
