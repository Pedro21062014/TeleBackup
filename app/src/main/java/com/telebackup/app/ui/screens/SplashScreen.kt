package com.telebackup.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telebackup.app.R
import com.telebackup.app.ui.theme.TelegramBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    darkTheme: Boolean,
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.72f) }
    val alpha = remember { Animatable(0f) }
    val infinite = rememberInfiniteTransition(label = "splash_pulse")
    val glow by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        delay(1150)
        alpha.animateTo(0f, tween(280))
        onFinished()
    }

    val bg = if (darkTheme) {
        Brush.verticalGradient(listOf(Color(0xFF0B0F1A), Color(0xFF101A2C), Color(0xFF0B1220)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF3F7FC), Color(0xFFE8F4FC), Color(0xFFF6FAFE)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .alpha(alpha.value),
        contentAlignment = Alignment.Center
    ) {
        // soft glow behind logo
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(1.15f)
                .alpha(glow * 0.35f)
                .clip(RoundedCornerShape(48.dp))
                .background(TelegramBlue.copy(alpha = 0.35f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "TeleBackup",
                modifier = Modifier
                    .size(112.dp)
                    .scale(scale.value)
                    .clip(RoundedCornerShape(28.dp))
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "TeleBackup",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = if (darkTheme) Color.White else Color(0xFF0B1220)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Backup para o Telegram",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) Color(0xFF9AA6BF) else Color(0xFF4A5A70)
            )
        }
    }
}
