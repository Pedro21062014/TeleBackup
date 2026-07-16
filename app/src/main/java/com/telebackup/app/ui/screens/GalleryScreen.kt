package com.telebackup.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.telebackup.app.MediaFilter
import com.telebackup.app.data.MediaItem
import com.telebackup.app.ui.components.EmptyState
import com.telebackup.app.ui.components.PrimaryButton
import com.telebackup.app.ui.components.SecondaryButton
import com.telebackup.app.ui.components.SectionHeader
import com.telebackup.app.ui.components.StatChip
import com.telebackup.app.ui.theme.ErrorRose
import com.telebackup.app.ui.theme.LocalAppSurfaces
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.util.ImageLoading

@Composable
fun GalleryScreen(
    media: List<MediaItem>,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    filter: MediaFilter,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onFilter: (MediaFilter) -> Unit,
    onToggle: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onOpen: (MediaItem) -> Unit,
    onBackup: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val surfaces = LocalAppSurfaces.current
    val photos = media.count { it.isImage }
    val videos = media.count { it.isVideo }
    val gridState = rememberLazyGridState()
    val selectedBytes = remember(selectedIds, media) {
        media.filter { it.id in selectedIds }.sumOf { it.size }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionHeader(
            title = if (selectionMode) "${selectedIds.size} selecionado(s)" else "Galeria",
            subtitle = if (selectionMode) "Envie ao Telegram ou apague do aparelho"
            else "Toque para ver · segure para selecionar · backup ou apagar",
            action = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar", tint = surfaces.textSecondary)
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(surfaces.elevated.copy(alpha = if (surfaces.isDark) 0.55f else 1f))
                .border(1.dp, surfaces.border.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Outlined.Storage, null, tint = TelegramBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "storage/emulated/0 · até 50 mil mídias",
                style = MaterialTheme.typography.bodySmall,
                color = surfaces.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Mídias", "${media.size}")
            StatChip("Fotos", "$photos")
            StatChip("Vídeos", "$videos", accent = Color(0xFFFF8A65))
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipMini("Tudo", filter == MediaFilter.All) { onFilter(MediaFilter.All) }
            FilterChipMini("Fotos", filter == MediaFilter.Photos) { onFilter(MediaFilter.Photos) }
            FilterChipMini("Vídeos", filter == MediaFilter.Videos) { onFilter(MediaFilter.Videos) }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                TextButton(onClick = onSelectAll) {
                    Icon(Icons.Outlined.SelectAll, null, tint = TelegramBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Todas", color = TelegramBlue)
                }
                if (selectionMode || selectedIds.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Icon(Icons.Outlined.Close, null, tint = surfaces.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Limpar", color = surfaces.textSecondary)
                    }
                }
            }
            if (selectedIds.isNotEmpty()) {
                Text(
                    MediaItem.formatSize(selectedBytes),
                    style = MaterialTheme.typography.labelLarge,
                    color = surfaces.textMuted
                )
            }
        }

        when {
            isLoading && media.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TelegramBlue)
                        Spacer(Modifier.height(12.dp))
                        Text("Lendo storage/emulated/0…", color = surfaces.textSecondary)
                    }
                }
            }
            media.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "Nenhuma mídia encontrada",
                    subtitle = "Permita acesso a fotos/vídeos. O app lê DCIM, Pictures, Download e Movies.",
                    actionLabel = "Atualizar",
                    onAction = onRefresh
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = media,
                        key = { it.id },
                        contentType = { if (it.isVideo) "video" else "image" }
                    ) { item ->
                        MediaThumb(
                            item = item,
                            selected = item.id in selectedIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) onToggle(item.id) else onOpen(item)
                            },
                            onLongClick = {
                                if (selectionMode) onToggle(item.id) else onEnterSelection(item.id)
                            },
                            onCheckClick = { onToggle(item.id) }
                        )
                    }
                }

                if (selectedIds.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryButton(
                            text = "Apagar ${selectedIds.size}",
                            onClick = onDelete,
                            icon = Icons.Outlined.DeleteOutline,
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            text = "Backup ${selectedIds.size}",
                            onClick = onBackup,
                            icon = Icons.Outlined.CloudUpload,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterChipMini(label: String, selected: Boolean, onClick: () -> Unit) {
    val surfaces = LocalAppSurfaces.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Outlined.FilterList, null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TelegramBlue.copy(alpha = 0.2f),
            selectedLabelColor = TelegramBlue,
            selectedLeadingIconColor = TelegramBlue,
            containerColor = surfaces.elevated,
            labelColor = surfaces.textSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = surfaces.border,
            selectedBorderColor = TelegramBlue.copy(alpha = 0.5f),
            enabled = true,
            selected = selected
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaThumb(
    item: MediaItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckClick: () -> Unit
) {
    val surfaces = LocalAppSurfaces.current
    val context = LocalContext.current
    val loader = remember(context) { ImageLoading.imageLoader(context) }
    val request = remember(item.uri, item.isVideo) {
        ImageLoading.thumbRequest(context, item.uri, isVideo = item.isVideo, sizePx = 420)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) TelegramBlue else surfaces.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(surfaces.elevated)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = item.name,
            imageLoader = loader,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(surfaces.elevated)
                )
            },
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (item.isVideo) Icons.Outlined.Videocam else Icons.Outlined.PhotoLibrary,
                        null,
                        tint = surfaces.textMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        )

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = "Vídeo",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            if (item.durationLabel.isNotBlank()) {
                Text(
                    item.durationLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Icon(
                Icons.Outlined.Videocam,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
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

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        selected -> TelegramBlue
                        selectionMode -> Color.Black.copy(alpha = 0.4f)
                        else -> Color.Black.copy(alpha = 0.25f)
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                .combinedClickable(onClick = onCheckClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
