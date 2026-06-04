package com.rork.neonhighwayracer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NeonDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF00E5FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF050510),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF004D66),
    secondary = androidx.compose.ui.graphics.Color(0xFFFF00FF),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF050510),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF4D0050),
    background = androidx.compose.ui.graphics.Color(0xFF050510),
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color(0xFF0D0D2B),
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A1040),
    onSurfaceVariant = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
    outline = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeonDarkColorScheme,
        content = content
    )
}
