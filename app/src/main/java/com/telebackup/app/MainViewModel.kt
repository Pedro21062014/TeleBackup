package com.telebackup.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telebackup.app.data.AppSettings
import com.telebackup.app.data.BackupProgress
import com.telebackup.app.data.BackupState
import com.telebackup.app.data.CloudMediaItem
import com.telebackup.app.data.CloudMediaStore
import com.telebackup.app.data.MediaItem
import com.telebackup.app.data.MediaRepository
import com.telebackup.app.data.MetadataOptions
import com.telebackup.app.data.TelegramUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiState(
    val media: List<MediaItem> = emptyList(),
    val isLoadingMedia: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val selectionMode: Boolean = false,
    val backup: BackupProgress = BackupProgress(),
    val testMessage: String? = null,
    val testOk: Boolean? = null,
    val isTesting: Boolean = false,
    val viewerItem: MediaItem? = null,
    val cloudViewer: CloudMediaItem? = null,
    val cloudFileUrl: String? = null,
    val isLoadingCloudUrl: Boolean = false,
    val snackbar: String? = null,
    val filter: MediaFilter = MediaFilter.All
)

enum class MediaFilter { All, Photos, Videos }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = (app as TeleBackupApp).preferences
    private val mediaRepo = MediaRepository(app)
    private val uploader = TelegramUploader(app)
    private val cloudStore = CloudMediaStore(app)

    val settings: StateFlow<AppSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val cloudItems: StateFlow<List<CloudMediaItem>> = cloudStore.items

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { cloudStore.load() }
    }

    fun saveConfig(token: String, chatId: String) {
        viewModelScope.launch {
            prefs.saveBotConfig(token, chatId)
            _ui.update { it.copy(snackbar = "Configuração salva") }
        }
    }

    fun saveMetadata(options: MetadataOptions) {
        viewModelScope.launch {
            prefs.saveMetadataOptions(options)
            _ui.update { it.copy(snackbar = "Opções de metadados salvas") }
        }
    }

    fun testConnection(token: String, chatId: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isTesting = true, testMessage = null, testOk = null) }
            val result = uploader.testConnection(token.trim(), chatId.trim())
            _ui.update {
                it.copy(
                    isTesting = false,
                    testOk = result.ok,
                    testMessage = result.message,
                    snackbar = result.message
                )
            }
            if (result.ok) {
                prefs.saveBotConfig(token.trim(), chatId.trim())
            }
        }
    }

    fun addFolder(uri: Uri) {
        viewModelScope.launch {
            mediaRepo.takePersistablePermission(uri)
            prefs.addFolder(uri.toString())
            _ui.update { it.copy(snackbar = "Pasta adicionada") }
            refreshMedia()
        }
    }

    fun removeFolder(uri: String) {
        viewModelScope.launch {
            prefs.removeFolder(uri)
            _ui.update { it.copy(snackbar = "Pasta removida") }
            refreshMedia()
        }
    }

    fun refreshMedia() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoadingMedia = true) }
            try {
                // Fast path: load device media first so UI populates quickly
                val fromDevice = mediaRepo.loadDeviceMedia(limit = 2000)
                _ui.update { state ->
                    val selected = if (!state.selectionMode) emptySet()
                    else state.selectedIds.intersect(fromDevice.map { it.id }.toSet())
                    state.copy(
                        media = fromDevice,
                        isLoadingMedia = false,
                        selectedIds = selected
                    )
                }
                // Merge optional SAF folders without blocking first paint
                val folders = settings.value.folderUris
                if (folders.isNotEmpty()) {
                    val fromFolders = mediaRepo.loadFromFolders(folders)
                    val merged = (fromFolders + fromDevice)
                        .distinctBy { "${it.uri}" }
                        .sortedByDescending { it.dateAdded }
                    _ui.update { state ->
                        val selected = if (!state.selectionMode) emptySet()
                        else state.selectedIds.intersect(merged.map { it.id }.toSet())
                        state.copy(media = merged, selectedIds = selected)
                    }
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isLoadingMedia = false,
                        snackbar = "Erro ao ler mídia: ${e.message}"
                    )
                }
            }
        }
    }

    fun setFilter(filter: MediaFilter) {
        _ui.update { it.copy(filter = filter) }
    }

    fun enterSelectionMode(id: Long? = null) {
        _ui.update { state ->
            state.copy(
                selectionMode = true,
                selectedIds = if (id != null) state.selectedIds + id else state.selectedIds
            )
        }
    }

    fun exitSelectionMode() {
        _ui.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun toggleSelect(id: Long) {
        _ui.update { state ->
            val next = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(
                selectedIds = next,
                selectionMode = next.isNotEmpty() || state.selectionMode
            )
        }
    }

    fun selectAllVisible(ids: List<Long>) {
        _ui.update { it.copy(selectedIds = ids.toSet(), selectionMode = true) }
    }

    fun clearSelection() {
        _ui.update { it.copy(selectedIds = emptySet(), selectionMode = false) }
    }

    fun openViewer(item: MediaItem) {
        _ui.update { it.copy(viewerItem = item) }
    }

    fun closeViewer() {
        _ui.update { it.copy(viewerItem = null) }
    }

    fun openCloudViewer(item: CloudMediaItem) {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    cloudViewer = item,
                    cloudFileUrl = null,
                    isLoadingCloudUrl = true
                )
            }
            val token = settings.value.botToken
            val prefer = item.thumbFileId.ifBlank { item.fileId }
            val url = if (token.isNotBlank() && prefer.isNotBlank()) {
                uploader.getFileUrl(token, prefer)
                    ?: if (item.fileId.isNotBlank() && item.fileId != prefer) {
                        uploader.getFileUrl(token, item.fileId)
                    } else null
            } else null
            // For full view prefer full file for images; for video use file
            val fullUrl = if (token.isNotBlank() && item.fileId.isNotBlank()) {
                uploader.getFileUrl(token, item.fileId) ?: url
            } else url
            // Prefer local uri if still available
            val local = item.localUri.takeIf { it.isNotBlank() }
            _ui.update {
                it.copy(
                    cloudFileUrl = fullUrl ?: local,
                    isLoadingCloudUrl = false
                )
            }
        }
    }

    fun closeCloudViewer() {
        _ui.update { it.copy(cloudViewer = null, cloudFileUrl = null, isLoadingCloudUrl = false) }
    }

    fun removeCloudItem(id: String) {
        viewModelScope.launch {
            cloudStore.remove(id)
            _ui.update { it.copy(snackbar = "Removido da galeria na nuvem") }
        }
    }

    fun clearCloudGallery() {
        viewModelScope.launch {
            cloudStore.clear()
            _ui.update { it.copy(snackbar = "Galeria na nuvem limpa") }
        }
    }

    fun startBackup() {
        val cfg = settings.value
        if (cfg.botToken.isBlank() || cfg.chatId.isBlank()) {
            _ui.update { it.copy(snackbar = "Configure o token e o Group ID primeiro") }
            return
        }
        val selected = _ui.value.media.filter { it.id in _ui.value.selectedIds }
        if (selected.isEmpty()) {
            _ui.update { it.copy(snackbar = "Selecione ao menos um item") }
            return
        }
        if (_ui.value.backup.state == BackupState.Uploading) return

        viewModelScope.launch {
            val result = uploader.backupAll(
                token = cfg.botToken,
                chatId = cfg.chatId,
                items = selected,
                options = cfg.metadata,
                onProgress = { progress -> _ui.update { it.copy(backup = progress) } },
                onUploaded = { cloud -> cloudStore.add(cloud) }
            )
            if (result.state == BackupState.Success) {
                val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                prefs.setLastBackup(now)
                _ui.update { it.copy(snackbar = result.message, selectionMode = false, selectedIds = emptySet()) }
            }
        }
    }

    fun clearSnackbar() {
        _ui.update { it.copy(snackbar = null) }
    }

    fun resetBackupState() {
        _ui.update { it.copy(backup = BackupProgress()) }
    }

    fun filteredMedia(): List<MediaItem> {
        val state = _ui.value
        return when (state.filter) {
            MediaFilter.All -> state.media
            MediaFilter.Photos -> state.media.filter { it.isImage }
            MediaFilter.Videos -> state.media.filter { it.isVideo }
        }
    }
}
