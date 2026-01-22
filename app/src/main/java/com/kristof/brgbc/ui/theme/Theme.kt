package com.kristof.brgbc.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BRGBCTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = TextPrimary,
        onPrimary = PureBlack,
        primaryContainer = CardBg,
        onPrimaryContainer = TextPrimary,
        
        secondary = AccentBlue,
        onSecondary = TextPrimary,
        secondaryContainer = CardBg,
        onSecondaryContainer = TextPrimary,
        
        background = PureBlack,
        onBackground = TextPrimary,
        
        surface = PureBlack,
        onSurface = TextPrimary,
        surfaceVariant = CardBg,
        onSurfaceVariant = TextSecondary,
        
        error = StatusRed,
        onError = TextPrimary,
        
        border = BorderColor,
        borderVariant = BorderLight
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}