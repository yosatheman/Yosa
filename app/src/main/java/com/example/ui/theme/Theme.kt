package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

// Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 / 48 (dp)
object DeepSpacing {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x12 = 48.dp
}

// Corner radii: 12 (chips), 16 (cards), 20 (sheets), 28 (hero), 999 (pills)
object DeepRadius {
    val chip = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    val sheet = RoundedCornerShape(20.dp)
    val sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val hero = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(999.dp)
}

val LocalDeepCurrentColors = staticCompositionLocalOf { DeepCurrentColors() }
val LocalFontSizeScale = staticCompositionLocalOf { 1.0f }

object DeepCurrentTheme {
    val colors: DeepCurrentColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDeepCurrentColors.current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val spacing = DeepSpacing
    val radius = DeepRadius
}

@Composable
fun DeepCurrentTheme(
    accent: AccentChoice = AccentChoice.CYAN,
    fontScale: Float = 1.0f,
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val deepColors = if (isDark) {
        DeepCurrentColors(accent = accent.color)
    } else {
        DeepCurrentColors(
            bgBase = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
            bgSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            bgElevated = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
            bgTint = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
            textHi = androidx.compose.ui.graphics.Color(0xFF0F172A),
            textMid = androidx.compose.ui.graphics.Color(0xFF475569),
            textLo = androidx.compose.ui.graphics.Color(0xFF94A3B8),
            textDis = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
            accent = accent.color,
            borderSubtle = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
        )
    }

    val materialColorScheme = if (isDark) {
        darkColorScheme(
            primary = deepColors.accent,
            onPrimary = deepColors.bgBase,
            primaryContainer = deepColors.bgTint,
            onPrimaryContainer = deepColors.textHi,
            secondary = deepColors.accent,
            onSecondary = deepColors.bgBase,
            background = deepColors.bgBase,
            onBackground = deepColors.textHi,
            surface = deepColors.bgSurface,
            onSurface = deepColors.textHi,
            surfaceVariant = deepColors.bgElevated,
            onSurfaceVariant = deepColors.textMid,
            error = deepColors.err,
            onError = deepColors.textHi,
            outline = deepColors.borderSubtle
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = deepColors.accent,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = deepColors.bgTint,
            onPrimaryContainer = deepColors.textHi,
            secondary = deepColors.accent,
            onSecondary = androidx.compose.ui.graphics.Color.White,
            background = deepColors.bgBase,
            onBackground = deepColors.textHi,
            surface = deepColors.bgSurface,
            onSurface = deepColors.textHi,
            surfaceVariant = deepColors.bgElevated,
            onSurfaceVariant = deepColors.textMid,
            error = deepColors.err,
            onError = androidx.compose.ui.graphics.Color.White,
            outline = deepColors.borderSubtle
        )
    }

    val typography = createDeepCurrentTypography(fontScale)

    CompositionLocalProvider(
        LocalDeepCurrentColors provides deepColors,
        LocalFontSizeScale provides fontScale
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = typography,
            content = content
        )
    }
}
