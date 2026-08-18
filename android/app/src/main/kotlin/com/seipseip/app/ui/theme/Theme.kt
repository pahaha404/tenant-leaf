package com.seipseip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = TenantLeafGreen,
    onPrimary = Color.White,
    primaryContainer = TenantLeafSoftGreen,
    onPrimaryContainer = TenantLeafDeepGreen,
    secondary = TenantLeafOrange,
    background = TenantLeafBackground,
    onBackground = TenantLeafText,
    surface = TenantLeafBackground,
    onSurface = TenantLeafText,
    onSurfaceVariant = TenantLeafSecondaryText,
)

private val DarkColors = darkColorScheme(
    primary = TenantLeafSoftGreen,
    onPrimary = TenantLeafDeepGreen,
    secondary = TenantLeafOrange,
)

@Composable
fun TenantLeafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
