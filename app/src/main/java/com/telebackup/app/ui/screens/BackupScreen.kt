package com.telebackup.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.telebackup.app.data.AppSettings
import com.telebackup.app.data.BackupProgress
import com.telebackup.app.data.BackupState
import com.telebackup.app.ui.components.PrimaryButton
import com.telebackup.app.ui.components.ProgressBlock
import com.telebackup.app.ui.components.PulsingDot
import com.telebackup.app.ui.components.SecondaryButton
import com.telebackup.app.ui.components.SectionCard
import com.telebackup.app.ui.components.SectionHeader
import com.telebackup.app.ui.components.StatChip
import com.telebackup.app.ui.theme.ErrorRose
import com.telebackup.app.ui.theme.NightBorder
import com.telebackup.app.ui.theme.NightElevated
import com.telebackup.app.ui.theme.SuccessGreen
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.ui.theme.TextMuted
import com.telebackup.app.ui.theme.TextSecondary
import com.telebackup.app.ui.theme.WarningAmber

@Composable
fun BackupScreen(
    settings: AppSettings,
    mediaCount: Int,
    selectedCount: Int,
    cloudCount: Int,
    backup: BackupProgress,
    batteryOptimized: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onGoConfig: () -> Unit,
    onGoCloud: () -> Unit,
    onFixBattery: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val configured = settings.isConfigured
    val isUploading = backup.state == BackupState.Uploading
    val meta = settings.metadata

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SectionHeader(
            title = "Backup",
            subtitle = "Notificação de progresso · roda em segundo plano"
        )

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF153A52), Color(0xFF1A2234), Color(0xFF142033))
                    )
                )
                .border(1.dp, TelegramBlue.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(TelegramBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (backup.state) {
                                BackupState.Success -> Icons.Outlined.CloudDone
                                BackupState.Error -> Icons.Outlined.ErrorOutline
                                else -> Icons.Outlined.CloudUpload
                            },
                            contentDescription = null,
                            tint = when (backup.state) {
                                BackupState.Success -> SuccessGreen
                                BackupState.Error -> ErrorRose
                                else -> TelegramBlue
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            when (backup.state) {
                                BackupState.Idle -> "Pronto para enviar"
                                BackupState.Scanning -> "Escaneando…"
                                BackupState.Uploading -> "Enviando…"
                                BackupState.Success -> "Backup concluído"
                                BackupState.Error -> "Algo deu errado"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isUploading) {
                                PulsingDot()
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                if (configured) "Bot configurado · fotos + vídeos" else "Configure o bot primeiro",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (configured) SuccessGreen else WarningAmber
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatChip("Locais", "$mediaCount")
                    StatChip("Selecionadas", "$selectedCount", accent = SuccessGreen)
                    StatChip("Nuvem", "$cloudCount", accent = TelegramBlue)
                }

                if (settings.lastBackupAt.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Último backup: ${settings.lastBackupAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Battery / background card
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.BatterySaver,
                    null,
                    tint = if (batteryOptimized) WarningAmber else SuccessGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (batteryOptimized) "Otimização de bateria ativa"
                        else "Segundo plano liberado",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (batteryOptimized)
                            "Desative a otimização para o backup não parar com a tela desligada"
                        else
                            "O app pode continuar enviando em segundo plano",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (batteryOptimized) {
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = "Desativar otimização de bateria",
                    onClick = onFixBattery,
                    icon = Icons.Outlined.BatterySaver
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = "Abrir configurações de bateria",
                    onClick = onOpenBatterySettings
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, null, tint = TelegramBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Durante o envio, uma notificação mostra o progresso (ex.: 12/50).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Metadata summary
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOff, null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Metadados ativos", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onGoConfig) {
                    Text("Editar", color = TelegramBlue)
                }
            }
            Spacer(Modifier.height(8.dp))
            val tags = buildList {
                if (meta.keepOriginalFile && !meta.stripAllExif && !meta.stripLocation && !meta.stripCameraInfo) {
                    add("Arquivo original")
                } else {
                    if (!meta.keepOriginalFile || meta.stripAllExif) add("EXIF limpo")
                    else {
                        if (meta.stripLocation) add("Sem GPS no arquivo")
                        if (meta.stripCameraInfo) add("Sem câmera no arquivo")
                    }
                }
                if (meta.includeLocationInCaption) add("GPS na legenda")
                if (meta.includeDateInCaption) add("Data na legenda")
                if (meta.includeCameraInCaption) add("Câmera na legenda")
            }
            Text(
                tags.joinToString(" · ").ifBlank { "Padrão" },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(16.dp))

        if (backup.state == BackupState.Uploading || backup.state == BackupState.Success || backup.state == BackupState.Error) {
            SectionCard {
                ProgressBlock(
                    current = backup.current,
                    total = backup.total,
                    message = backup.message.ifBlank { "Processando…" },
                    fileName = backup.currentFile
                )
                if (backup.state == BackupState.Success || backup.state == BackupState.Error) {
                    Spacer(Modifier.height(14.dp))
                    SecondaryButton(text = "Limpar status", onClick = onReset)
                    if (backup.state == BackupState.Success && cloudCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton(
                            text = "Ver galeria na nuvem",
                            onClick = onGoCloud,
                            icon = Icons.Outlined.Cloud
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (!configured) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Settings, null, tint = WarningAmber)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Configuração incompleta", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Informe o token do bot e o Group ID",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    text = "Ir para Config",
                    onClick = onGoConfig,
                    icon = Icons.Outlined.Settings
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        PrimaryButton(
            text = if (isUploading) "Enviando…" else "Iniciar backup",
            onClick = onStart,
            enabled = configured && selectedCount > 0 && !isUploading,
            loading = isUploading,
            icon = Icons.Outlined.CloudUpload
        )

        Spacer(Modifier.height(18.dp))

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Dicas", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                TipLine("A notificação de progresso fica na barra enquanto envia.")
                TipLine("Desative a otimização de bateria para segundo plano estável.")
                TipLine("Toque numa mídia para visualizar; segure para selecionar.")
                TipLine("Itens enviados entram na aba Nuvem automaticamente.")
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NightElevated.copy(alpha = 0.5f))
                .border(1.dp, NightBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Videocam, null, tint = TextSecondary)
            Spacer(Modifier.width(10.dp))
            Text(
                "Limite Bot API: fotos ~10 MB · vídeos/docs ~50 MB. Arquivos maiores vão como documento quando possível.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NightElevated.copy(alpha = 0.5f))
                .border(1.dp, NightBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Speed, null, tint = TextSecondary)
            Spacer(Modifier.width(10.dp))
            Text(
                "Intervalo suave entre envios para respeitar os limites da API do Telegram.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TipLine(text: String) {
    Row {
        Text("•", color = TelegramBlue)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
