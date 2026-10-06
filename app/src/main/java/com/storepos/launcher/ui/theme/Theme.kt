package com.storepos.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StoreBlue = Color(0xFF0B4FD8)
val StoreBlueBright = Color(0xFF1677FF)
val StoreTeal = Color(0xFF08B7A7)
val StoreInk = Color(0xFF10213A)
val StoreMuted = Color(0xFF607086)
val StoreSurface = Color(0xFFF5F8FC)

private val StorePosLight = lightColorScheme(
    primary = StoreBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F1FF),
    onPrimaryContainer = Color(0xFF0A357F),
    secondary = StoreTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF9F5),
    onSecondaryContainer = Color(0xFF075F58),
    tertiary = Color(0xFF20A66A),
    background = Color.White,
    onBackground = StoreInk,
    surface = Color.White,
    onSurface = StoreInk,
    surfaceVariant = StoreSurface,
    onSurfaceVariant = StoreMuted,
    outline = Color(0xFFDCE4EF),
    outlineVariant = Color(0xFFE8EEF6)
)

@Composable
fun StorePosLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StorePosLight, content = content)
}
