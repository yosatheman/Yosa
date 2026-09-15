package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Background Tokens
val BgBase = Color(0xFF0A0C10)
val BgSurface = Color(0xFF12151C)
val BgElevated = Color(0xFF1A1F29)
val BgTint = Color(0xFF0F1620)

// Text Tokens
val TextHi = Color(0xFFF2F4F8)
val TextMid = Color(0xFFA6AEBD)
val TextLo = Color(0xFF5C6472)
val TextDis = Color(0xFF3A4150)

// Semantic Tokens
val SemanticOk = Color(0xFF22C55E)
val SemanticWarn = Color(0xFFF59E0B)
val SemanticErr = Color(0xFFEF4444)
val SemanticInfo = Color(0xFF38BDF8)

// Border Subtle Token (1px rgba white 6%)
val SubtleBorderColor = Color(0x0FFFFFFF)

// User-Swappable Accents
enum class AccentChoice(val label: String, val color: Color) {
    CYAN("Cyan", Color(0xFF22D3EE)),
    VIOLET("Violet", Color(0xFFA78BFA)),
    AMBER("Amber", Color(0xFFF59E0B)),
    ROSE("Rose", Color(0xFFFB7185)),
    MINT("Mint", Color(0xFF34D399));

    companion object {
        fun fromName(name: String): AccentChoice {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CYAN
        }
    }
}

@Immutable
data class DeepCurrentColors(
    val bgBase: Color = BgBase,
    val bgSurface: Color = BgSurface,
    val bgElevated: Color = BgElevated,
    val bgTint: Color = BgTint,
    val textHi: Color = TextHi,
    val textMid: Color = TextMid,
    val textLo: Color = TextLo,
    val textDis: Color = TextDis,
    val accent: Color = AccentChoice.CYAN.color,
    val ok: Color = SemanticOk,
    val warn: Color = SemanticWarn,
    val err: Color = SemanticErr,
    val info: Color = SemanticInfo,
    val borderSubtle: Color = SubtleBorderColor
)
