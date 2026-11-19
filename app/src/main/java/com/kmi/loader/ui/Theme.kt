package com.kmi.loader.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 与视觉稿一致的调色板。 */
object KColors {
    val Background = Color(0xFFF6F5FA)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF17161B)
    val Ink2 = Color(0xFF6E6C78)
    val Ink3 = Color(0xFF9C9AA8)
    val Line = Color(0xFFEFEDF5)
    val Primary = Color(0xFF3A56E0)
    val PrimaryDeep = Color(0xFF2846C8)
    val PrimaryLight = Color(0xFF5470F2)
    val PrimaryBg = Color(0xFFE7EBFD)
    val Green = Color(0xFF12A150)
    val GreenText = Color(0xFF0E8C46)
    val GreenBg = Color(0xFFE8F6EE)
    val Amber = Color(0xFFD9930F)
    val AmberText = Color(0xFFB67A05)
    val AmberBg = Color(0xFFFDF6E7)
    val Red = Color(0xFFE5484D)
    val RedText = Color(0xFFC93A3F)
    val RedBg = Color(0xFFFCE9EA)
    val Track = Color(0xFFF2F1F8)
    val TrackDeep = Color(0xFFECEBF3)
    val Disabled = Color(0xFFB9B7C4)
}

object KType {
    val Mono = FontFamily.Monospace
    val MonoValue = TextStyle(
        fontFamily = Mono,
        fontSize = 13.sp,
        letterSpacing = (-0.2).sp,
    )
}

private val KShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun KmiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = KColors.Primary,
            onPrimary = Color.White,
            background = KColors.Background,
            onBackground = KColors.Ink,
            surface = KColors.Card,
            onSurface = KColors.Ink,
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
            bodyMedium = TextStyle(fontSize = 13.sp),
        ),
        shapes = KShapes,
        content = content,
    )
}
