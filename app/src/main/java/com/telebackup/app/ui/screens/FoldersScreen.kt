package com.telebackup.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telebackup.app.ui.components.EmptyState
import com.telebackup.app.ui.components.PrimaryButton
import com.telebackup.app.ui.components.SectionCard
import com.telebackup.app.ui.components.SectionHeader
import com.telebackup.app.ui.theme.ErrorRose
import com.telebackup.app.ui.theme.NightBorder
import com.telebackup.app.ui.theme.NightElevated
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.ui.theme.TextMuted
import com.telebackup.app.ui.theme.TextSecondary

@Composable
fun FoldersScreen(
    folderUris: Set<String>,
    onAddFolder: (Uri) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRefresh: () -> Unit = {}
) {
    // onRefresh reserved for pull-to-refresh if needed
    @Suppress("UNUSED_PARAMETER")
    val _refresh = onRefresh
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            onAddFolder(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SectionHeader(
            title = "Pastas",
            subtitle = "Escolha pastas para incluir no backup"
        )
        Spacer(Modifier.height(16.dp))

        PrimaryButton(
            text = "Adicionar pasta",
            onClick = { picker.launch(null) },
            icon = Icons.Outlined.CreateNewFolder
        )

        Spacer(Modifier.height(16.dp))

        if (folderUris.isEmpty()) {
            SectionCard {
                EmptyState(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Nenhuma pasta ainda",
                    subtitle = "Adicione pastas de fotos, downloads ou DCIM",
                    actionLabel = "Escolher pasta",
                    onAction = { picker.launch(null) }
                )
            }
        } else {
            Text(
                "${folderUris.size} pasta(s) selecionada(s)",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(folderUris.toList(), key = { it }) { uriStr ->
                    FolderRow(
                        uriStr = uriStr,
                        onRemove = { onRemoveFolder(uriStr) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun FolderRow(uriStr: String, onRemove: () -> Unit) {
    val name = try {
        val uri = Uri.parse(uriStr)
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        docId.substringAfterLast(':').ifBlank { uri.lastPathSegment ?: "Pasta" }
    } catch (_: Exception) {
        uriStr.takeLast(40)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NightElevated.copy(alpha = 0.65f))
            .border(1.dp, NightBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TelegramBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = TelegramBlue)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Acesso persistente · somente leitura",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remover", tint = ErrorRose)
        }
    }
}
