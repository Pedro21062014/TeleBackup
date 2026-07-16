package com.telebackup.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.telebackup.app.data.CleanupAnalyzer
import com.telebackup.app.data.MediaItem
import com.telebackup.app.ui.components.EmptyState
import com.telebackup.app.ui.components.PrimaryButton
import com.telebackup.app.ui.components.SectionCard
import com.telebackup.app.ui.components.SectionHeader
import com.telebackup.app.ui.components.StatChip
import com.telebackup.app.ui.theme.ErrorRose
import com.telebackup.app.ui.theme.LocalAppSurfaces
import com.telebackup.app.ui.theme.SuccessGreen
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.util.ImageLoading

enum class CleanupMode { Duplicates, Categories }

@Composable
fun CleanupScreen(
    media: List<MediaItem>,
    isLoading: Boolean,
    isAnalyzing: Boolean,
    duplicates: List<CleanupAnalyzer.DuplicateGroup>,
    categories: List<CleanupAnalyzer.CategoryBucket>,
    selectedIds: Set<Long>,
    selectedCategoryId: String?,
    mode: CleanupMode,
    onMode: (CleanupMode) -> Unit,
    onRefresh: () -> Unit,
    onAnalyze: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onToggle: (Long) -> Unit,
    onSelectExtras: (List<MediaItem>) -> Unit,
    onSelectAllInCategory: (List<MediaItem>) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val surfaces = LocalAppSurfaces.current
    val waste = remember(duplicates) { CleanupAnalyzer.totalWaste(duplicates) }
    val selectedCount = selectedIds.size
    val selectedBytes = remember(selectedIds, media) {
        media.filter { it.id in selectedIds }.sumOf { it.size }
    }
    val activeCategory = categories.firstOrNull { it.id == selectedCategoryId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionHeader(
            title = "Limpar",
            subtitle = "Duplicatas e categorias · ${media.size} mídias indexadas",
            action = {
                IconButton(onClick = {
                    onRefresh()
                    onAnalyze()
                }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar", tint = TelegramBlue)
                }
            }
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip("Duplicatas", mode == CleanupMode.Duplicates) { onMode(CleanupMode.Duplicates) }
            ModeChip("Categorias", mode == CleanupMode.Categories) { onMode(CleanupMode.Categories) }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Mídias", "${media.size}")
            if (mode == CleanupMode.Duplicates) {
                StatChip("Grupos", "${duplicates.size}", accent = ErrorRose)
                StatChip("Economia", MediaItem.formatSize(waste), accent = SuccessGreen)
            } else {
                StatChip("Categorias", "${categories.size}", accent = TelegramBlue)
                StatChip("Selecionadas", "$selectedCount", accent = ErrorRose)
            }
        }

        Spacer(Modifier.height(10.dp))

        when {
            isLoading || isAnalyzing -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TelegramBlue)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (isLoading) "Lendo biblioteca de mídia…" else "Analisando duplicatas e categorias…",
                            color = surfaces.textSecondary
                        )
                    }
                }
            }
            media.isEmpty() -> {
                SectionCard {
                    EmptyState(
                        icon = Icons.Outlined.CleaningServices,
                        title = "Nenhuma mídia",
                        subtitle = "Conceda permissão de fotos/vídeos para limpar o aparelho",
                        actionLabel = "Atualizar",
                        onAction = onRefresh
                    )
                }
            }
            mode == CleanupMode.Duplicates -> {
                if (duplicates.isEmpty()) {
                    SectionCard {
                        EmptyState(
                            icon = Icons.Outlined.ContentCopy,
                            title = "Nenhuma duplicata encontrada",
                            subtitle = "Não achamos arquivos com o mesmo nome e tamanho",
                            actionLabel = "Analisar de novo",
                            onAction = onAnalyze
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Text(
                                "Mantemos a cópia mais recente. Selecione as extras para apagar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = surfaces.textMuted
                            )
                        }
                        items(duplicates, key = { it.key }) { group ->
                            DuplicateCard(
                                group = group,
                                selectedIds = selectedIds,
                                onToggle = onToggle,
                                onSelectExtras = { onSelectExtras(group.extras) }
                            )
                        }
                    }
                }
            }
            else -> {
                // Categories
                Column(modifier = Modifier.weight(1f)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            CategoryChip(
                                title = "${cat.emoji} ${cat.title}",
                                count = cat.count,
                                selected = cat.id == selectedCategoryId,
                                onClick = {
                                    onSelectCategory(if (cat.id == selectedCategoryId) null else cat.id)
                                }
                            )
                        }
                    }

                    if (activeCategory == null) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(categories, key = { it.id }) { cat ->
                                CategoryRow(
                                    category = cat,
                                    onClick = { onSelectCategory(cat.id) }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${activeCategory.emoji} ${activeCategory.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = surfaces.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${activeCategory.count} · ${activeCategory.sizeLabel} · ${activeCategory.subtitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = surfaces.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { onSelectAllInCategory(activeCategory.items) }) {
                                Text("Todas", color = TelegramBlue)
                            }
                            TextButton(onClick = { onSelectCategory(null) }) {
                                Text("Voltar", color = surfaces.textSecondary)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activeCategory.items, key = { it.id }) { item ->
                                SelectableThumb(
                                    item = item,
                                    selected = item.id in selectedIds,
                                    onToggle = { onToggle(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedCount > 0) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClearSelection) {
                    Icon(Icons.Outlined.Close, null, tint = surfaces.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Limpar", color = surfaces.textSecondary)
                }
                PrimaryButton(
                    text = "Apagar $selectedCount (${MediaItem.formatSize(selectedBytes)})",
                    onClick = onDeleteSelected,
                    icon = Icons.Outlined.DeleteOutline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val surfaces = LocalAppSurfaces.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TelegramBlue.copy(alpha = 0.18f),
            selectedLabelColor = TelegramBlue,
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

@Composable
private fun CategoryChip(title: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val surfaces = LocalAppSurfaces.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                "$title ($count)",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TelegramBlue.copy(alpha = 0.18f),
            selectedLabelColor = TelegramBlue,
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

@Composable
private fun CategoryRow(category: CleanupAnalyzer.CategoryBucket, onClick: () -> Unit) {
    val surfaces = LocalAppSurfaces.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (surfaces.isDark) surfaces.elevated.copy(alpha = 0.65f) else Color.White)
            .border(1.dp, surfaces.border.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TelegramBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(category.emoji, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                category.title,
                style = MaterialTheme.typography.titleMedium,
                color = surfaces.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${category.count} itens · ${category.sizeLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = surfaces.textMuted
            )
        }
        Icon(Icons.Outlined.Folder, null, tint = surfaces.textMuted)
    }
}

@Composable
private fun DuplicateCard(
    group: CleanupAnalyzer.DuplicateGroup,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onSelectExtras: () -> Unit
) {
    val surfaces = LocalAppSurfaces.current
    SectionCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.keep.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = surfaces.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${group.count} cópias · pode liberar ${MediaItem.formatSize(group.wasteBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaces.textMuted
                    )
                }
                TextButton(onClick = onSelectExtras) {
                    Text("Selecionar extras", color = ErrorRose)
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(group.items, key = { it.id }) { item ->
                    val isKeep = item.id == group.keep.id
                    Box {
                        SelectableThumb(
                            item = item,
                            selected = item.id in selectedIds,
                            onToggle = { onToggle(item.id) },
                            modifier = Modifier.width(96.dp)
                        )
                        if (isKeep) {
                            Text(
                                "Manter",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(4.dp)
                                    .background(SuccessGreen.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableThumb(
    item: MediaItem,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalAppSurfaces.current
    val context = LocalContext.current
    val loader = remember(context) { ImageLoading.imageLoader(context) }
    val request = remember(item.uri) {
        ImageLoading.thumbRequest(context, item.uri, isVideo = item.isVideo, sizePx = 360)
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) TelegramBlue else surfaces.border.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(surfaces.elevated)
            .clickable(onClick = onToggle)
    ) {
        AsyncImage(
            model = request,
            contentDescription = item.name,
            imageLoader = loader,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) TelegramBlue else Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            item.sizeLabel,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
