package com.example.aihymnbook

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Sacred Sanctuary Palette
val SanctuaryNavy = Color(0xFF1B263B)
val SanctuaryNavyLight = Color(0xFF415A77)
val AntiqueGold = Color(0xFFC59B27)
val AntiqueGoldContainer = Color(0xFFF6E8C1)
val ParchmentLight = Color(0xFFFAF8F5)
val ParchmentCard = Color(0xFFFFFFFF)
val WarmCharcoal = Color(0xFF1E1E24)

val MidnightBg = Color(0xFF0F141D)
val MidnightCard = Color(0xFF192231)
val SoftGoldDark = Color(0xFFE5C158)

private val LightSanctuaryColors = lightColorScheme(
    primary = SanctuaryNavy,
    onPrimary = Color.White,
    primaryContainer = AntiqueGoldContainer,
    onPrimaryContainer = SanctuaryNavy,
    secondary = AntiqueGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE6D6),
    onSecondaryContainer = WarmCharcoal,
    tertiary = SanctuaryNavyLight,
    onTertiary = Color.White,
    background = ParchmentLight,
    onBackground = WarmCharcoal,
    surface = ParchmentCard,
    onSurface = WarmCharcoal,
    surfaceVariant = Color(0xFFF0ECE1),
    onSurfaceVariant = Color(0xFF4A473E),
    outline = Color(0xFFD0C8B6)
)

private val DarkSanctuaryColors = darkColorScheme(
    primary = SoftGoldDark,
    onPrimary = MidnightBg,
    primaryContainer = Color(0xFF3A3118),
    onPrimaryContainer = SoftGoldDark,
    secondary = Color(0xFF778DA9),
    onSecondary = MidnightBg,
    background = MidnightBg,
    onBackground = Color(0xFFECEAE4),
    surface = MidnightCard,
    onSurface = Color(0xFFECEAE4),
    surfaceVariant = Color(0xFF232E40),
    onSurfaceVariant = Color(0xFFB8C0CC),
    outline = Color(0xFF3E4C63)
)

val HymnTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun AIHymnBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkSanctuaryColors else LightSanctuaryColors
    MaterialTheme(
        colorScheme = colors,
        typography = HymnTypography,
        content = content
    )
}
