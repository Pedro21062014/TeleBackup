package com.telebackup.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand
val TelegramBlue = Color(0xFF2AABEE)
val TelegramBlueDark = Color(0xFF1A8FD1)
val AccentTeal = Color(0xFF00C2A8)
val AccentPurple = Color(0xFF7C5CFF)
val SuccessGreen = Color(0xFF2DB87A)
val WarningAmber = Color(0xFFE89B0D)
val ErrorRose = Color(0xFFE04565)

// Dark surfaces
val NightBg = Color(0xFF0B0F1A)
val NightSurface = Color(0xFF141A28)
val NightCard = Color(0xFF1A2234)
val NightElevated = Color(0xFF222B40)
val NightBorder = Color(0xFF2C3650)
val TextPrimary = Color(0xFFF2F5FA)
val TextSecondary = Color(0xFF9AA6BF)
val TextMuted = Color(0xFF6B7690)

// Light surfaces — high contrast for readability
val LightBg = Color(0xFFF3F6FB)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightElevated = Color(0xFFEEF3FA)
val LightBorder = Color(0xFFC5D0E0)
val LightTextPrimary = Color(0xFF0B1220)
val LightTextSecondary = Color(0xFF3D4A60)
val LightTextMuted = Color(0xFF66758C)

data class AppSurfaceColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val elevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val fieldBg: Color,
    val fieldText: Color,
    val fieldLabel: Color,
    val isDark: Boolean
)

val LocalAppSurfaces = staticCompositionLocalOf {
    AppSurfaceColors(
        background = LightBg,
        surface = LightSurface,
        card = LightCard,
        elevated = LightElevated,
        border = LightBorder,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textMuted = LightTextMuted,
        fieldBg = Color(0xFFF7F9FC),
        fieldText = LightTextPrimary,
        fieldLabel = LightTextSecondary,
        isDark = false
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
    onPrimaryContainer = Color(0xFF002B40),
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
    outlineVariant = Color(0xFFE2E8F2),
    error = ErrorRose,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5EA),
    onErrorContainer = Color(0xFF5C1020)
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
            fieldBg = NightElevated.copy(alpha = 0.55f),
            fieldText = Color.White,
            fieldLabel = TextSecondary,
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
            fieldBg = Color(0xFFF7F9FC),
            fieldText = LightTextPrimary,
            fieldLabel = LightTextSecondary,
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
