package com.telebackup.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telebackup.app.ui.theme.LocalAppSurfaces
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.update.UpdatePhase
import com.telebackup.app.update.UpdateUiState

@Composable
fun UpdateBanner(
    state: UpdateUiState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val surfaces = LocalAppSurfaces.current
    val show = state.visible && state.phase != UpdatePhase.Idle && state.phase != UpdatePhase.UpToDate

    AnimatedVisibility(
        visible = show,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        if (surfaces.isDark) listOf(Color(0xFF102033), Color(0xFF0F1A2A))
                        else listOf(Color(0xFFE8F5FE), Color(0xFFDDF0FC))
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (surfaces.isDark) Color(0xFF152338) else Color.White)
                    .border(1.dp, TelegramBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TelegramBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (state.phase) {
                            UpdatePhase.Downloading -> Icons.Outlined.CloudDownload
                            else -> Icons.Outlined.SystemUpdateAlt
                        },
                        contentDescription = null,
                        tint = TelegramBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        when (state.phase) {
                            UpdatePhase.Checking -> "Procurando atualização…"
                            UpdatePhase.Available -> "Nova versão ${state.info?.tag ?: ""}"
                            UpdatePhase.Downloading -> "Baixando atualização…"
                            UpdatePhase.ReadyToInstall -> "Pronto para instalar"
                            UpdatePhase.Installing -> "Abrindo instalador…"
                            UpdatePhase.Error -> "Falha na atualização"
                            else -> "Atualização"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = surfaces.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when {
                            state.message.isNotBlank() -> state.message
                            state.phase == UpdatePhase.Available ->
                                "Toque em Atualizar para baixar e instalar"
                            state.phase == UpdatePhase.Downloading ->
                                progressLabel(state)
                            state.phase == UpdatePhase.ReadyToInstall ->
                                "Toque em Instalar para concluir"
                            else -> "TeleBackup"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaces.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (state.phase == UpdatePhase.Downloading || state.phase == UpdatePhase.Checking) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (state.phase == UpdatePhase.Checking) 0f
                                else state.progress.coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = TelegramBlue,
                            trackColor = surfaces.border
                        )
                        if (state.phase == UpdatePhase.Downloading) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${(state.progress * 100).toInt()}% · ${progressLabel(state)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TelegramBlue
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                when (state.phase) {
                    UpdatePhase.Available -> {
                        TextButton(onClick = onDownload) {
                            Text("Atualizar", color = TelegramBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    UpdatePhase.ReadyToInstall -> {
                        TextButton(onClick = onInstall) {
                            Text("Instalar", color = TelegramBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    UpdatePhase.Error -> {
                        TextButton(onClick = onRetry) {
                            Text("Tentar", color = TelegramBlue)
                        }
                    }
                    UpdatePhase.Downloading, UpdatePhase.Checking, UpdatePhase.Installing -> {
                        // no action
                    }
                    else -> Unit
                }

                if (state.phase != UpdatePhase.Downloading && state.phase != UpdatePhase.Installing) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Fechar",
                            tint = surfaces.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun progressLabel(state: UpdateUiState): String {
    fun fmt(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> "%.1f KB".format(b / 1024.0)
        else -> "%.1f MB".format(b / (1024.0 * 1024))
    }
    return if (state.totalBytes > 0) {
        "${fmt(state.downloadedBytes)} / ${fmt(state.totalBytes)}"
    } else if (state.downloadedBytes > 0) {
        fmt(state.downloadedBytes)
    } else {
        "Preparando…"
    }
}
