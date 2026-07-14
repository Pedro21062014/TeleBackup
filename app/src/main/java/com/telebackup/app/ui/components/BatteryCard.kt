package com.telebackup.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telebackup.app.ui.theme.NightBorder
import com.telebackup.app.ui.theme.SuccessGreen
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.ui.theme.TextMuted
import com.telebackup.app.ui.theme.TextSecondary
import com.telebackup.app.ui.theme.WarningAmber
import com.telebackup.app.util.BatteryOptimization

/**
 * Clean battery / background card.
 * Uses Activity context so Android shows the **native** unrestricted-background dialog.
 */
@Composable
fun BatteryPermissionCard(
    batteryOptimized: Boolean,
    onStatusMaybeChanged: () -> Unit = {}
) {
    val context = LocalContext.current

    SectionCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (batteryOptimized) WarningAmber.copy(alpha = 0.15f)
                            else SuccessGreen.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (batteryOptimized) Icons.Outlined.BatterySaver else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (batteryOptimized) WarningAmber else SuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (batteryOptimized) "Segundo plano bloqueado"
                        else "Segundo plano liberado",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (batteryOptimized)
                            "Permita uso sem restrição de bateria"
                        else
                            "Backup pode continuar com a tela off",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (batteryOptimized) {
                // Primary CTA — short label so it never stacks over itself
                Button(
                    onClick = {
                        BatteryOptimization.requestIgnoreBatteryOptimizations(context)
                        onStatusMaybeChanged()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Permitir segundo plano",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        BatteryOptimization.openBatterySettings(context)
                        onStatusMaybeChanged()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, NightBorder)
                ) {
                    Text(
                        "Abrir ajustes do sistema",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "O Android abre um aviso nativo. Toque em Permitir / Sem restrições.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 3
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Tudo certo — o envio pode rodar em segundo plano.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = TelegramBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Durante o backup, a notificação mostra o progresso (ex.: 12/50).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 3
                )
            }
        }
    }
}
