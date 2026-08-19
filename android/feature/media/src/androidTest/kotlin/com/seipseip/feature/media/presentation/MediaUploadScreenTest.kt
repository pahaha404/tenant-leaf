package com.seipseip.feature.media.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaUploadScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permissionScreen_allowsPickerFallback() {
        var pickerClicks = 0
        composeRule.setContent {
            MediaUploadScreen(
                state = MediaUploadUiState.PermissionRequired,
                onBack = {},
                onRequestPermission = {},
                onRetry = {},
                onUseNewest = {},
                onPickVideo = { pickerClicks += 1 },
            )
        }

        composeRule.onNodeWithText("최근 임장 영상을 찾으려면 동영상 접근 권한이 필요합니다.").assertIsDisplayed()
        composeRule.onNodeWithText("영상 직접 선택").performClick()
        assertEquals(1, pickerClicks)
    }

    @Test
    fun uploadScreen_showsProgress() {
        composeRule.setContent {
            MediaUploadScreen(
                state = MediaUploadUiState.Uploading(completed = 7, total = 20),
                onBack = {},
                onRequestPermission = {},
                onRetry = {},
                onUseNewest = {},
                onPickVideo = {},
            )
        }

        composeRule.onNodeWithText("사진을 안전하게 전송하는 중입니다.").assertIsDisplayed()
        composeRule.onNodeWithText("7 / 20").assertIsDisplayed()
    }
}
