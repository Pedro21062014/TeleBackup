package com.telebackup.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.telebackup.app.data.CloudMediaItem
import com.telebackup.app.ui.components.EmptyState
import com.telebackup.app.ui.components.SectionCard
import com.telebackup.app.ui.components.SectionHeader
import com.telebackup.app.ui.components.StatChip
import com.telebackup.app.ui.theme.LocalAppSurfaces
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.util.ImageLoading

@Composable
fun CloudGalleryScreen(
    items: List<CloudMediaItem>,
    isConfigured: Boolean,
    isSyncing: Boolean = false,
    onOpen: (CloudMediaItem) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onSync: () -> Unit = {},
    onGoConfig: () -> Unit
) {
    val surfaces = LocalAppSurfaces.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionHeader(
            title = "Nuvem",
            subtitle = "Persiste no Telegram · reabre com as mesmas credenciais",
            action = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isConfigured) {
                        IconButton(onClick = onSync, enabled = !isSyncing) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = TelegramBlue
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = "Sincronizar",
                                    tint = TelegramBlue
                                )
                            }
                        }
                    }
                    if (items.isNotEmpty()) {
                        TextButton(onClick = onClear) {
                            Text("Limpar", color = ErrorRose)
                        }
                    }
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip("Na nuvem", "${items.size}", accent = TelegramBlue)
            StatChip("Fotos", "${items.count { !it.isVideo }}")
            StatChip("Vídeos", "${items.count { it.isVideo }}", accent = Color(0xFFFF8A65))
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Cada item mostra a data de envio. Após reinstalar o app, cole o mesmo token e Group ID e toque em atualizar.",
            style = MaterialTheme.typography.bodySmall,
            color = surfaces.textMuted
        )

        Spacer(Modifier.height(12.dp))

        when {
            !isConfigured -> {
                SectionCard {
                    EmptyState(
                        icon = Icons.Outlined.CloudOff,
                        title = "Configure o bot primeiro",
                        subtitle = "Com token e Group ID, a galeria é restaurada do Telegram",
                        actionLabel = "Ir para Config",
                        onAction = onGoConfig
                    )
                }
            }
            isSyncing && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TelegramBlue)
                        Spacer(Modifier.height(12.dp))
                        Text("Sincronizando nuvem…", color = surfaces.textSecondary)
                    }
                }
            }
            items.isEmpty() -> {
                SectionCard {
                    EmptyState(
                        icon = Icons.Outlined.Cloud,
                        title = "Nada na nuvem ainda",
                        subtitle = "Faça um backup — o índice fica fixado no chat e reaparece após reinstalar",
                        actionLabel = if (isConfigured) "Sincronizar" else null,
                        onAction = if (isConfigured) onSync else null
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        CloudThumb(item = item, onClick = { onOpen(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudThumb(item: CloudMediaItem, onClick: () -> Unit) {
    val surfaces = LocalAppSurfaces.current
    val context = LocalContext.current
    val loader = androidx.compose.runtime.remember(context) { ImageLoading.imageLoader(context) }
    val model: Any? = when {
        item.localUri.isNotBlank() -> Uri.parse(item.localUri)
        else -> null
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, surfaces.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(surfaces.elevated)
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = item.name,
                imageLoader = loader,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    if (item.isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                    contentDescription = null,
                    tint = TelegramBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(TelegramBlue.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Cloud, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }

        if (item.isVideo) {
            Icon(
                Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            )
        }

        // Date badge
        if (item.dateShort.isNotBlank()) {
            Text(
                item.dateShort,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))
                )
                .padding(6.dp)
        ) {
            Column {
                Text(
                    item.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.dateLabel.isNotBlank()) {
                    Text(
                        item.dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
