package com.telebackup.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand
val TelegramBlue = Color(0xFF2AABEE)
val TelegramBlueDark = Color(0xFF229ED9)
val AccentTeal = Color(0xFF00C2A8)
val AccentPurple = Color(0xFF7C5CFF)
val SuccessGreen = Color(0xFF3DDC97)
val WarningAmber = Color(0xFFFFB020)
val ErrorRose = Color(0xFFFF5C7A)

// Dark surfaces
val NightBg = Color(0xFF0B0F1A)
val NightSurface = Color(0xFF141A28)
val NightCard = Color(0xFF1A2234)
val NightElevated = Color(0xFF222B40)
val NightBorder = Color(0xFF2C3650)
val TextPrimary = Color(0xFFF2F5FA)
val TextSecondary = Color(0xFF9AA6BF)
val TextMuted = Color(0xFF6B7690)

// Light surfaces
val LightBg = Color(0xFFF4F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightElevated = Color(0xFFF0F4FA)
val LightBorder = Color(0xFFD5DEEB)
val LightTextPrimary = Color(0xFF0F1524)
val LightTextSecondary = Color(0xFF5A657A)
val LightTextMuted = Color(0xFF8A94A8)

data class AppSurfaceColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val elevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isDark: Boolean
)

val LocalAppSurfaces = staticCompositionLocalOf {
    AppSurfaceColors(
        background = NightBg,
        surface = NightSurface,
        card = NightCard,
        elevated = NightElevated,
        border = NightBorder,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textMuted = TextMuted,
        isDark = true
    )
}

private val DarkColors = darkColorScheme(
    primary = TelegramBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF153A52),
    onPrimaryContainer = Color(0xFFB8E6FF),
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0E3B36),
    onSecondaryContainer = Color(0xFFA8F0E5),
    tertiary = AccentPurple,
    onTertiary = Color.White,
    background = NightBg,
    onBackground = TextPrimary,
    surface = NightSurface,
    onSurface = TextPrimary,
    surfaceVariant = NightCard,
    onSurfaceVariant = TextSecondary,
    outline = NightBorder,
    outlineVariant = Color(0xFF1F2738),
    error = ErrorRose,
    onError = Color.White,
    errorContainer = Color(0xFF4A1522),
    onErrorContainer = Color(0xFFFFD0D8)
)

private val LightColors = lightColorScheme(
    primary = TelegramBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F0FF),
    onPrimaryContainer = Color(0xFF00344D),
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F5EE),
    onSecondaryContainer = Color(0xFF003730),
    tertiary = AccentPurple,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFE8EDF5),
    error = ErrorRose,
    onError = Color.White
)

@Composable
fun TeleBackupTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = darkTheme
    val colors = if (useDark) DarkColors else LightColors
    val surfaces = if (useDark) {
        AppSurfaceColors(
            background = NightBg,
            surface = NightSurface,
            card = NightCard,
            elevated = NightElevated,
            border = NightBorder,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textMuted = TextMuted,
            isDark = true
        )
    } else {
        AppSurfaceColors(
            background = LightBg,
            surface = LightSurface,
            card = LightCard,
            elevated = LightElevated,
            border = LightBorder,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textMuted = LightTextMuted,
            isDark = false
        )
    }
    CompositionLocalProvider(LocalAppSurfaces provides surfaces) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            content = content
        )
    }
}
