package com.telebackup.app.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.telebackup.app.MediaFilter
import com.telebackup.app.UiState
import com.telebackup.app.data.AppSettings
import com.telebackup.app.data.CloudMediaItem
import com.telebackup.app.data.MediaItem
import com.telebackup.app.data.MetadataOptions
import com.telebackup.app.ui.components.GradientBackground
import com.telebackup.app.ui.screens.BackupScreen
import com.telebackup.app.ui.screens.CloudGalleryScreen
import com.telebackup.app.ui.screens.CloudViewerScreen
import com.telebackup.app.ui.screens.FoldersScreen
import com.telebackup.app.ui.screens.GalleryScreen
import com.telebackup.app.ui.screens.PhotoViewerScreen
import com.telebackup.app.ui.screens.SettingsScreen
import com.telebackup.app.ui.theme.LocalAppSurfaces
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.util.BatteryOptimization
import com.telebackup.app.util.Haptics

private data class Tab(val label: String, val icon: ImageVector)

@Composable
fun TeleBackupAppRoot(
    settings: AppSettings,
    ui: UiState,
    cloudItems: List<CloudMediaItem>,
    filteredMedia: List<MediaItem>,
    onSaveConfig: (String, String) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onSaveMetadata: (MetadataOptions) -> Unit,
    onAddFolder: (Uri) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRefresh: () -> Unit,
    onFilter: (MediaFilter) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOpenViewer: (MediaItem) -> Unit,
    onCloseViewer: () -> Unit,
    onOpenCloud: (CloudMediaItem) -> Unit,
    onCloseCloud: () -> Unit,
    onCloudPageChanged: (CloudMediaItem) -> Unit,
    onRemoveCloud: (String) -> Unit,
    onClearCloud: () -> Unit,
    onSyncCloud: () -> Unit,
    onStartBackup: () -> Unit,
    onClearSnackbar: () -> Unit,
    onResetBackup: () -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onDismissBatteryDialog: () -> Unit,
    onContinueAfterBattery: () -> Unit,
    onToggleTheme: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val view = LocalView.current
    val surfaces = LocalAppSurfaces.current

    val tabs = listOf(
        Tab("Galeria", Icons.Outlined.PhotoLibrary),
        Tab("Nuvem", Icons.Outlined.Cloud),
        Tab("Pastas", Icons.Outlined.FolderOpen),
        Tab("Backup", Icons.Outlined.CloudUpload),
        Tab("Config", Icons.Outlined.Settings)
    )

    LaunchedEffect(ui.snackbar) {
        ui.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            onClearSnackbar()
        }
    }

    if (ui.showBatteryDialog) {
        AlertDialog(
            onDismissRequest = onDismissBatteryDialog,
            title = { Text("Permitir segundo plano") },
            text = {
                Text(
                    "O Android precisa autorizar o TeleBackup a rodar em segundo plano " +
                        "para o backup não parar com a tela desligada.\n\n" +
                        "Toque em “Permitir” para abrir o aviso nativo do sistema."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    BatteryOptimization.requestIgnoreBatteryOptimizations(context)
                    onRequestBatteryUnrestricted()
                }) {
                    Text("Permitir", color = TelegramBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = onContinueAfterBattery) {
                    Text("Agora não", color = surfaces.textMuted)
                }
            },
            containerColor = surfaces.surface,
            titleContentColor = surfaces.textPrimary,
            textContentColor = surfaces.textMuted
        )
    }

    if (ui.viewerItem != null) {
        PhotoViewerScreen(
            items = filteredMedia.ifEmpty { ui.media },
            initial = ui.viewerItem,
            onClose = onCloseViewer
        )
        return
    }

    if (ui.cloudViewer != null) {
        CloudViewerScreen(
            items = cloudItems,
            initial = ui.cloudViewer,
            urlByFileId = ui.cloudUrlByFileId,
            loadingIds = ui.cloudLoadingIds,
            onClose = onCloseCloud,
            onRemove = { item ->
                onRemoveCloud(item.id)
                onCloseCloud()
            },
            onPageChanged = onCloudPageChanged
        )
        return
    }

    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = surfaces.surface,
                        contentColor = surfaces.textPrimary,
                        actionColor = TelegramBlue
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = surfaces.surface.copy(alpha = 0.98f),
                    contentColor = surfaces.textPrimary,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, t ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = {
                                if (tab != index) {
                                    Haptics.tabSwitch(context, view)
                                    tab = index
                                    if (index == 1 && settings.isConfigured) {
                                        onSyncCloud()
                                    }
                                }
                            },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = {
                                Text(t.label, style = MaterialTheme.typography.labelMedium)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TelegramBlue,
                                selectedTextColor = TelegramBlue,
                                unselectedIconColor = surfaces.textMuted,
                                unselectedTextColor = surfaces.textMuted,
                                indicatorColor = TelegramBlue.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 4 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { it / 4 } + fadeOut())
                        }
                    },
                    label = "tabs"
                ) { current ->
                    when (current) {
                        0 -> GalleryScreen(
                            media = filteredMedia,
                            selectedIds = ui.selectedIds,
                            selectionMode = ui.selectionMode,
                            filter = ui.filter,
                            isLoading = ui.isLoadingMedia,
                            onRefresh = onRefresh,
                            onFilter = onFilter,
                            onToggle = onToggleSelect,
                            onEnterSelection = onEnterSelection,
                            onSelectAll = onSelectAll,
                            onClear = onClearSelection,
                            onOpen = onOpenViewer,
                            onBackup = {
                                Haptics.tabSwitch(context, view)
                                tab = 3
                            }
                        )
                        1 -> CloudGalleryScreen(
                            items = cloudItems,
                            isConfigured = settings.isConfigured,
                            isSyncing = ui.isSyncingCloud,
                            onOpen = onOpenCloud,
                            onRemove = onRemoveCloud,
                            onClear = onClearCloud,
                            onSync = onSyncCloud,
                            onGoConfig = {
                                Haptics.tabSwitch(context, view)
                                tab = 4
                            }
                        )
                        2 -> FoldersScreen(
                            folderUris = settings.folderUris,
                            onAddFolder = onAddFolder,
                            onRemoveFolder = onRemoveFolder
                        )
                        3 -> BackupScreen(
                            settings = settings,
                            mediaCount = ui.media.size,
                            selectedCount = ui.selectedIds.size,
                            cloudCount = cloudItems.size,
                            backup = ui.backup,
                            batteryOptimized = ui.batteryOptimized,
                            onStart = onStartBackup,
                            onReset = onResetBackup,
                            onGoConfig = {
                                Haptics.tabSwitch(context, view)
                                tab = 4
                            },
                            onGoCloud = {
                                Haptics.tabSwitch(context, view)
                                tab = 1
                            },
                            onBatteryStatusRefresh = { }
                        )
                        else -> SettingsScreen(
                            settings = settings,
                            isTesting = ui.isTesting,
                            testOk = ui.testOk,
                            testMessage = ui.testMessage,
                            batteryOptimized = ui.batteryOptimized,
                            onSave = onSaveConfig,
                            onTest = onTestConnection,
                            onSaveMetadata = onSaveMetadata,
                            onBatteryStatusRefresh = { },
                            onToggleTheme = onToggleTheme
                        )
                    }
                }
            }
        }
    }
}
