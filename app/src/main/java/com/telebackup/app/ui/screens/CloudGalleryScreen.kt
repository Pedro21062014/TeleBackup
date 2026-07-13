package com.telebackup.app.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SdStorage
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.telebackup.app.data.CloudMediaItem
import com.telebackup.app.ui.components.EmptyState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudGalleryScreen(
    items: List<CloudMediaItem>,
    isConfigured: Boolean,
    onOpen: (CloudMediaItem) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onGoConfig: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionHeader(
            title = "Nuvem",
            subtitle = "Mídias enviadas ao Telegram por este app",
            action = {
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Limpar", color = ErrorRose)
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

        Spacer(Modifier.height(14.dp))

        when {
            !isConfigured -> {
                SectionCard {
                    EmptyState(
                        icon = Icons.Outlined.CloudOff,
                        title = "Configure o bot primeiro",
                        subtitle = "Com token e Group ID, os backups aparecem aqui",
                        actionLabel = "Ir para Config",
                        onAction = onGoConfig
                    )
                }
            }
            items.isEmpty() -> {
                SectionCard {
                    EmptyState(
                        icon = Icons.Outlined.Cloud,
                        title = "Nada na nuvem ainda",
                        subtitle = "Faça um backup na Galeria — os itens enviados ficam listados aqui",
                        actionLabel = null,
                        onAction = null
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
    val model: Any? = when {
        item.localUri.isNotBlank() -> Uri.parse(item.localUri)
        else -> null
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, NightBorder.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(NightElevated)
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = item.name,
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

        // cloud badge
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
                )
                .padding(6.dp)
        ) {
            Text(
                item.name,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CloudViewerScreen(
    item: CloudMediaItem,
    fileUrl: String?,
    isLoading: Boolean,
    onClose: () -> Unit,
    onRemove: () -> Unit
) {
    BackHandler(onBack = onClose)
    val dateLabel = rememberDate(item.uploadedAt)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TelegramBlue)
                }
            }
            fileUrl != null -> {
                if (item.isVideo) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(fileUrl))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = fileUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CloudOff, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Não foi possível carregar a mídia", color = TextSecondary)
                        Text("Arquivo local pode ter sido removido", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Outlined.Close, null, tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .padding(6.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Enviado · $dateLabel", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.DeleteOutline, "Remover", tint = ErrorRose)
            }
        }

        // bottom info
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                .padding(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(Icons.Outlined.SdStorage, item.sizeLabel)
                MetaChip(
                    if (item.isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                    if (item.isVideo) "Vídeo" else "Foto"
                )
                if (item.hasLocation && item.latitude != null) {
                    MetaChip(
                        Icons.Outlined.LocationOn,
                        "%.4f, %.4f".format(item.latitude, item.longitude)
                    )
                }
            }
            if (item.caption.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    item.caption,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun rememberDate(ts: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))
}
