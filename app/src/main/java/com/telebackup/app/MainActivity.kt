package com.telebackup.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.telebackup.app.service.BackupNotifier
import com.telebackup.app.ui.TeleBackupAppRoot
import com.telebackup.app.ui.theme.TeleBackupTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            viewModel.refreshMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BackupNotifier.ensureChannels(this)
        requestMediaPermissions()

        setContent {
            TeleBackupTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val settings by viewModel.settings.collectAsStateWithLifecycle()
                    val ui by viewModel.ui.collectAsStateWithLifecycle()
                    val cloud by viewModel.cloudItems.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.refreshMedia()
                        viewModel.refreshBatteryStatus()
                    }

                    val filtered = when (ui.filter) {
                        MediaFilter.All -> ui.media
                        MediaFilter.Photos -> ui.media.filter { it.isImage }
                        MediaFilter.Videos -> ui.media.filter { it.isVideo }
                    }

                    TeleBackupAppRoot(
                        settings = settings,
                        ui = ui,
                        cloudItems = cloud,
                        filteredMedia = filtered,
                        onSaveConfig = viewModel::saveConfig,
                        onTestConnection = viewModel::testConnection,
                        onSaveMetadata = viewModel::saveMetadata,
                        onAddFolder = viewModel::addFolder,
                        onRemoveFolder = viewModel::removeFolder,
                        onRefresh = viewModel::refreshMedia,
                        onFilter = viewModel::setFilter,
                        onToggleSelect = viewModel::toggleSelect,
                        onEnterSelection = { id -> viewModel.enterSelectionMode(id) },
                        onSelectAll = { viewModel.selectAllVisible(filtered.map { it.id }) },
                        onClearSelection = viewModel::clearSelection,
                        onOpenViewer = viewModel::openViewer,
                        onCloseViewer = viewModel::closeViewer,
                        onOpenCloud = viewModel::openCloudViewer,
                        onCloseCloud = viewModel::closeCloudViewer,
                        onRemoveCloud = viewModel::removeCloudItem,
                        onClearCloud = viewModel::clearCloudGallery,
                        onStartBackup = viewModel::startBackup,
                        onClearSnackbar = viewModel::clearSnackbar,
                        onResetBackup = viewModel::resetBackupState,
                        onRequestBatteryUnrestricted = viewModel::requestBatteryUnrestricted,
                        onOpenBatterySettings = viewModel::openBatterySettings,
                        onDismissBatteryDialog = viewModel::dismissBatteryDialog,
                        onContinueAfterBattery = viewModel::continueBackupAfterBatteryPrompt
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBatteryStatus()
    }

    private fun requestMediaPermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed += Manifest.permission.READ_MEDIA_IMAGES
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed += Manifest.permission.READ_MEDIA_VIDEO
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed += Manifest.permission.POST_NOTIFICATIONS
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed += Manifest.permission.READ_EXTERNAL_STORAGE
            }
            // Android 13+ only for notifications, but keep safe on 12-
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            viewModel.refreshMedia()
        }
    }
}
