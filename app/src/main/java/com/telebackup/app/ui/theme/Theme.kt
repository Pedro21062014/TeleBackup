package com.telebackup.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern minimalist dark-first palette inspired by Telegram blue
val TelegramBlue = Color(0xFF2AABEE)
val TelegramBlueDark = Color(0xFF229ED9)
val AccentTeal = Color(0xFF00C2A8)
val AccentPurple = Color(0xFF7C5CFF)
val SuccessGreen = Color(0xFF3DDC97)
val WarningAmber = Color(0xFFFFB020)
val ErrorRose = Color(0xFFFF5C7A)

val NightBg = Color(0xFF0B0F1A)
val NightSurface = Color(0xFF141A28)
val NightCard = Color(0xFF1A2234)
val NightElevated = Color(0xFF222B40)
val NightBorder = Color(0xFF2C3650)
val TextPrimary = Color(0xFFF2F5FA)
val TextSecondary = Color(0xFF9AA6BF)
val TextMuted = Color(0xFF6B7690)

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
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF0F1524),
    surface = Color.White,
    onSurface = Color(0xFF0F1524),
    surfaceVariant = Color(0xFFEEF2F8),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFFD0D7E4),
    outlineVariant = Color(0xFFE8EDF5),
    error = ErrorRose,
    onError = Color.White
)

@Composable
fun TeleBackupTheme(
    darkTheme: Boolean = true, // default dark for modern look
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
