package com.seipseip.app

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import com.seipseip.app.navigation.Route
import com.seipseip.app.navigation.AppNavGraph

@Composable
internal fun TenantLeafApp() {
    val navController = rememberNavController()
    var nickname by rememberSaveable { mutableStateOf("민지") }
    var lastHomeBackAt by rememberSaveable { mutableLongStateOf(0L) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val currentRoute by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val isHome = currentRoute?.destination?.route == Route.Home
    BackHandler(enabled = isHome) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastHomeBackAt <= 1_500L) {
            showExitDialog = true
        } else {
            lastHomeBackAt = now
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { androidx.compose.material3.Text("앱을 종료하시겠습니까?") },
            text = { androidx.compose.material3.Text("세입세잎을 종료할까요?") },
            confirmButton = {
                TextButton(onClick = { (context as? Activity)?.finish() }) { androidx.compose.material3.Text("예") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { androidx.compose.material3.Text("아니오") }
            },
        )
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 4.dp).navigationBarsPadding(),
            color = Color(0xFFFCFBF8),
        ) {
            AppNavGraph(
                navController = navController,
                nickname = nickname,
                onNicknameChanged = { nickname = it },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TenantLeafAppPreview() {
    TenantLeafApp()
}