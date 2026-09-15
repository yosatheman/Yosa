package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

fun createDeepCurrentTypography(fontScale: Float = 1.0f): Typography {
    fun scale(sp: Float): TextUnit = (sp * fontScale).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = scale(32f),
            lineHeight = scale(38f),
            color = TextHi
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = scale(22f),
            lineHeight = scale(28f),
            color = TextHi
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = scale(16f),
            lineHeight = scale(22f),
            color = TextHi
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scale(15f),
            lineHeight = scale(22f),
            color = TextMid
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = scale(14f),
            lineHeight = scale(20f),
            color = TextMid
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = scale(13f),
            lineHeight = scale(18f),
            color = TextLo
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = scale(13f),
            lineHeight = scale(20f),
            color = TextHi
        )
    )
}

val DeepCurrentMonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 20.sp,
    color = TextHi
)
