package com.telebackup.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.telebackup.app.R
import com.telebackup.app.data.AppSettings
import com.telebackup.app.data.MetadataOptions
import com.telebackup.app.ui.components.AppTextField
import com.telebackup.app.ui.components.BatteryPermissionCard
import com.telebackup.app.ui.components.PrimaryButton
import com.telebackup.app.ui.components.SecondaryButton
import com.telebackup.app.ui.components.SectionCard
import com.telebackup.app.ui.components.SectionHeader
import com.telebackup.app.ui.theme.ErrorRose
import com.telebackup.app.ui.theme.LocalAppSurfaces
import com.telebackup.app.ui.theme.SuccessGreen
import com.telebackup.app.ui.theme.TelegramBlue

@Composable
fun SettingsScreen(
    settings: AppSettings,
    isTesting: Boolean,
    testOk: Boolean?,
    testMessage: String?,
    batteryOptimized: Boolean = false,
    onSave: (String, String) -> Unit,
    onTest: (String, String) -> Unit,
    onSaveMetadata: (MetadataOptions) -> Unit,
    onFixBattery: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = {},
    onBatteryStatusRefresh: () -> Unit = {},
    onToggleTheme: () -> Unit = {}
) {
    var token by remember { mutableStateOf(settings.botToken) }
    var chatId by remember { mutableStateOf(settings.chatId) }
    var showToken by remember { mutableStateOf(false) }
    var meta by remember { mutableStateOf(settings.metadata) }
    val surfaces = LocalAppSurfaces.current

    LaunchedEffect(settings.botToken, settings.chatId) {
        if (token.isEmpty()) token = settings.botToken
        if (chatId.isEmpty()) chatId = settings.chatId
    }
    LaunchedEffect(settings.metadata) {
        meta = settings.metadata
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("TeleBackup", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("v1.4 · fotos, vídeos e nuvem", style = MaterialTheme.typography.bodyMedium, color = surfaces.textSecondary)
            }
            TextButton(onClick = onToggleTheme) {
                Text(
                    if (settings.darkTheme) "Tema claro" else "Tema escuro",
                    color = TelegramBlue
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionCard {
            MetaSwitch(
                icon = Icons.Outlined.Info,
                title = if (settings.darkTheme) "Tema escuro" else "Tema claro",
                subtitle = "Toque no switch para alternar",
                checked = !settings.darkTheme,
                onChecked = { onToggleTheme() }
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionHeader(
            title = "Bot do Telegram",
            subtitle = "Token do @BotFather e o ID do grupo/chat"
        )
        Spacer(Modifier.height(12.dp))

        SectionCard {
            Column {
                AppTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = "Token do Bot",
                    placeholder = "123456:ABC-DEF...",
                    isPassword = !showToken,
                    leadingIcon = Icons.Outlined.Key,
                    keyboardType = KeyboardType.Ascii
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Mostrar token",
                            tint = TextSecondary
                        )
                    }
                }

                AppTextField(
                    value = chatId,
                    onValueChange = { chatId = it },
                    label = "Group / Chat ID",
                    placeholder = "-1001234567890",
                    leadingIcon = Icons.Outlined.Tag,
                    keyboardType = KeyboardType.Text
                )

                Spacer(Modifier.height(16.dp))

                PrimaryButton(
                    text = "Testar conexão",
                    onClick = { onTest(token, chatId) },
                    loading = isTesting,
                    icon = Icons.Outlined.VerifiedUser,
                    enabled = token.isNotBlank() && chatId.isNotBlank()
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    text = "Salvar configuração",
                    onClick = { onSave(token, chatId) },
                    icon = Icons.Outlined.Save,
                    enabled = token.isNotBlank() && chatId.isNotBlank()
                )

                if (testMessage != null) {
                    Spacer(Modifier.height(14.dp))
                    StatusBanner(ok = testOk == true, message = testMessage)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionHeader(
            title = "Segundo plano",
            subtitle = "Permissão nativa do Android para não pausar o backup"
        )
        Spacer(Modifier.height(12.dp))
        BatteryPermissionCard(
            batteryOptimized = batteryOptimized,
            onStatusMaybeChanged = onBatteryStatusRefresh
        )

        Spacer(Modifier.height(24.dp))

        SectionHeader(
            title = "Metadados no envio",
            subtitle = "Controle o que vai no arquivo e na legenda"
        )
        Spacer(Modifier.height(12.dp))

        SectionCard {
            Column {
                Text(
                    "Arquivo (privacidade)",
                    style = MaterialTheme.typography.titleMedium,
                    color = surfaces.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ao desligar, o app regrava a imagem removendo dados EXIF sensíveis antes do upload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaces.textMuted
                )
                Spacer(Modifier.height(12.dp))

                MetaSwitch(
                    icon = Icons.Outlined.Security,
                    title = "Manter arquivo original",
                    subtitle = "Envia o arquivo sem alterar EXIF",
                    checked = meta.keepOriginalFile,
                    onChecked = {
                        meta = meta.copy(keepOriginalFile = it)
                        if (it) {
                            // when keeping original, clear strip flags visually optional
                        }
                        onSaveMetadata(meta.copy(keepOriginalFile = it))
                    }
                )
                MetaSwitch(
                    icon = Icons.Outlined.LocationOff,
                    title = "Remover localização (GPS)",
                    subtitle = "Apaga coordenadas EXIF do arquivo",
                    checked = meta.stripLocation || !meta.keepOriginalFile || meta.stripAllExif,
                    enabled = meta.keepOriginalFile && !meta.stripAllExif,
                    onChecked = {
                        val next = meta.copy(stripLocation = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )
                MetaSwitch(
                    icon = Icons.Outlined.CameraAlt,
                    title = "Remover info da câmera",
                    subtitle = "Marca, modelo e software",
                    checked = meta.stripCameraInfo || !meta.keepOriginalFile || meta.stripAllExif,
                    enabled = meta.keepOriginalFile && !meta.stripAllExif,
                    onChecked = {
                        val next = meta.copy(stripCameraInfo = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )
                MetaSwitch(
                    icon = Icons.Outlined.Security,
                    title = "Remover todo EXIF",
                    subtitle = "GPS, câmera, data e comentários",
                    checked = meta.stripAllExif || !meta.keepOriginalFile,
                    enabled = meta.keepOriginalFile,
                    onChecked = {
                        val next = meta.copy(stripAllExif = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = surfaces.border.copy(alpha = 0.6f)
                )

                Text(
                    "Legenda no Telegram",
                    style = MaterialTheme.typography.titleMedium,
                    color = surfaces.textPrimary
                )
                Spacer(Modifier.height(8.dp))

                MetaSwitch(
                    icon = Icons.Outlined.Title,
                    title = "Nome do arquivo",
                    checked = meta.includeFileNameInCaption,
                    onChecked = {
                        val next = meta.copy(includeFileNameInCaption = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )
                MetaSwitch(
                    icon = Icons.Outlined.Folder,
                    title = "Pasta de origem",
                    checked = meta.includeFolderInCaption,
                    onChecked = {
                        val next = meta.copy(includeFolderInCaption = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )
                MetaSwitch(
                    icon = Icons.Outlined.SdStorage,
                    title = "Tamanho do arquivo",
                    checked = meta.includeSizeInCaption,
                    onChecked = {
                        val next = meta.copy(includeSizeInCaption = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )
                MetaSwitch(
                    icon = Icons.Outlined.Schedule,
                    title = "Data da mídia",
                    checked = meta.includeDateInCaption,
                    onChecked = {
                        val next = meta.copy(includeDateInCaption = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )
                MetaSwitch(
                    icon = Icons.Outlined.CameraAlt,
                    title = "Câmera (marca/modelo)",
                    checked = meta.includeCameraInCaption,
                    onChecked = {
                        val next = meta.copy(includeCameraInCaption = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )
                MetaSwitch(
                    icon = Icons.Outlined.LocationOn,
                    title = "Localização na legenda",
                    subtitle = "Texto + link do Google Maps (não grava no arquivo)",
                    checked = meta.includeLocationInCaption,
                    onChecked = {
                        val next = meta.copy(includeLocationInCaption = it)
                        meta = next
                        onSaveMetadata(next)
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionHeader(title = "Como configurar", subtitle = "Passo a passo rápido")
        Spacer(Modifier.height(12.dp))

        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StepRow(1, "Abra o @BotFather no Telegram e crie um bot com /newbot")
                StepRow(2, "Copie o token e cole no campo acima")
                StepRow(3, "Adicione o bot ao seu grupo e torne-o admin (enviar mídia)")
                StepRow(4, "Descubra o Group ID (ex: @userinfobot) e cole aqui")
                StepRow(5, "Ajuste metadados, teste a conexão e faça o backup")
            }
        }

        if (settings.lastBackupAt.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = TelegramBlue)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Último backup", color = surfaces.textMuted, style = MaterialTheme.typography.labelMedium)
                        Text(settings.lastBackupAt, color = surfaces.textPrimary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "v1.4.0 · Nuvem restaura com as mesmas credenciais",
            style = MaterialTheme.typography.bodySmall,
            color = surfaces.textMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MetaSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onChecked: (Boolean) -> Unit
) {
    val surfaces = LocalAppSurfaces.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(surfaces.elevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (enabled) TelegramBlue else TextMuted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) Color.White else TextMuted,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(subtitle, color = surfaces.textMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TelegramBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = surfaces.border
            )
        )
    }
}

@Composable
private fun StatusBanner(ok: Boolean, message: String) {
    val bg = if (ok) SuccessGreen.copy(alpha = 0.12f) else ErrorRose.copy(alpha = 0.12f)
    val border = if (ok) SuccessGreen.copy(alpha = 0.4f) else ErrorRose.copy(alpha = 0.4f)
    val icon = if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline
    val tint = if (ok) SuccessGreen else ErrorRose
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(10.dp))
        Text(message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StepRow(n: Int, text: String) {
    val surfaces = LocalAppSurfaces.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(surfaces.elevated)
                .border(1.dp, surfaces.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$n", color = TelegramBlue, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            color = surfaces.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
