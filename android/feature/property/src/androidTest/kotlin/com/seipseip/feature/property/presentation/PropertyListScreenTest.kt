package com.seipseip.feature.property.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PropertyListScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun emptyState_displaysEmptyMessage() {
        composeRule.onNodeWithTag("property-list-empty").assertIsDisplayed()
        composeRule.onNodeWithText("등록된 매물이 없습니다.").assertIsDisplayed()
    }

    @Test
    fun addButton_invokesCreateCallback() {
        composeRule.onNodeWithTag("property-list-add").performClick()

        composeRule.runOnIdle {
            assertEquals(1, composeRule.activity.createRequests)
        }
    }
}
