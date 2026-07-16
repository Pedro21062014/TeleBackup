package com.telebackup.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telebackup.app.data.AppSettings
import com.telebackup.app.data.BackupProgress
import com.telebackup.app.data.BackupState
import com.telebackup.app.data.CloudIndexRemote
import com.telebackup.app.data.CloudMediaItem
import com.telebackup.app.data.CloudMediaStore
import com.telebackup.app.data.MediaItem
import com.telebackup.app.data.MediaRepository
import com.telebackup.app.data.MetadataOptions
import com.telebackup.app.data.TelegramUploader
import com.telebackup.app.service.BackupRuntime
import com.telebackup.app.service.BackupService
import com.telebackup.app.update.AppUpdateManager
import com.telebackup.app.update.UpdatePhase
import com.telebackup.app.update.UpdateUiState
import com.telebackup.app.util.BatteryOptimization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
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
    val cloudUrlByFileId: Map<String, String> = emptyMap(),
    val cloudLoadingIds: Set<String> = emptySet(),
    val isSyncingCloud: Boolean = false,
    val snackbar: String? = null,
    val filter: MediaFilter = MediaFilter.All,
    val batteryOptimized: Boolean = true,
    val showBatteryDialog: Boolean = false,
    val update: UpdateUiState = UpdateUiState()
)

enum class MediaFilter { All, Photos, Videos }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext = app.applicationContext
    private val prefs = (app as TeleBackupApp).preferences
    private val mediaRepo = MediaRepository(app)
    private val uploader = TelegramUploader(app)
    private val cloudStore = CloudMediaStore(app)
    private val cloudIndex = CloudIndexRemote(app)
    private val updateManager = AppUpdateManager(app)
    private var downloadedApk: File? = null

    val settings: StateFlow<AppSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val cloudItems: StateFlow<List<CloudMediaItem>> = cloudStore.items

    private val _ui = MutableStateFlow(
        UiState(
            batteryOptimized = !BatteryOptimization.isIgnoringBatteryOptimizations(appContext)
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { cloudStore.load() }
        // Auto-check for updates shortly after launch
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800)
            checkForAppUpdate(silent = true)
        }

        // When credentials become available, restore remote cloud index
        viewModelScope.launch {
            settings
                .map { it.botToken to it.chatId }
                .distinctUntilChanged()
                .collect { (token, chatId) ->
                    if (token.isNotBlank() && chatId.isNotBlank()) {
                        syncCloudFromTelegram(silent = true)
                    }
                }
        }

        viewModelScope.launch {
            BackupRuntime.progress.collect { progress ->
                _ui.update { it.copy(backup = progress) }
            }
        }
        viewModelScope.launch {
            BackupRuntime.uploaded.collect {
                cloudStore.load()
            }
        }
        viewModelScope.launch {
            BackupRuntime.finished.collect { result ->
                _ui.update { it.copy(backup = result) }
                cloudStore.load()
                if (result.state == BackupState.Success) {
                    val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    prefs.setLastBackup(now)
                    _ui.update {
                        it.copy(
                            snackbar = result.message,
                            selectionMode = false,
                            selectedIds = emptySet()
                        )
                    }
                    // Ensure remote index is published (service also does this)
                    publishCloudIndex()
                } else if (result.state == BackupState.Error) {
                    _ui.update { it.copy(snackbar = result.message) }
                }
            }
        }
    }

    fun refreshBatteryStatus() {
        val optimized = !BatteryOptimization.isIgnoringBatteryOptimizations(appContext)
        _ui.update { state ->
            if (!optimized && state.showBatteryDialog && BackupRuntime.pendingJob != null) {
                state.copy(batteryOptimized = false, showBatteryDialog = false)
            } else {
                state.copy(batteryOptimized = optimized)
            }
        }
        if (!optimized && BackupRuntime.pendingJob != null && !BackupRuntime.isRunning.value) {
            if (!_ui.value.showBatteryDialog) {
                launchPendingBackup()
            }
        }
    }

    fun dismissBatteryDialog() {
        BackupRuntime.pendingJob = null
        _ui.update { it.copy(showBatteryDialog = false) }
    }

    fun requestBatteryUnrestricted() {
        BatteryOptimization.requestIgnoreBatteryOptimizations(appContext)
    }

    fun openBatterySettings() {
        BatteryOptimization.openBatterySettings(appContext)
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            prefs.setDarkTheme(dark)
            _ui.update { it.copy(snackbar = if (dark) "Tema escuro" else "Tema claro") }
        }
    }

    fun saveConfig(token: String, chatId: String) {
        viewModelScope.launch {
            prefs.saveBotConfig(token, chatId)
            _ui.update { it.copy(snackbar = "Configuração salva · sincronizando nuvem…") }
            syncCloudFromTelegram(silent = false)
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
                syncCloudFromTelegram(silent = false)
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

    fun syncCloudFromTelegram(silent: Boolean = false) {
        val cfg = settings.value
        if (cfg.botToken.isBlank() || cfg.chatId.isBlank()) {
            if (!silent) _ui.update { it.copy(snackbar = "Configure token e Group ID") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(isSyncingCloud = true) }
            try {
                val fetched = cloudIndex.fetchIndex(
                    token = cfg.botToken,
                    chatId = cfg.chatId,
                    knownFileId = cfg.cloudIndexFileId
                )
                if (fetched != null && fetched.items.isNotEmpty()) {
                    cloudStore.addAll(fetched.items, replace = false)
                    if (fetched.messageId > 0 || fetched.fileId.isNotBlank()) {
                        prefs.setCloudIndexMeta(fetched.messageId, fetched.fileId)
                    }
                    if (!silent) {
                        _ui.update {
                            it.copy(
                                snackbar = "Nuvem restaurada: ${fetched.items.size} item(ns) com data"
                            )
                        }
                    }
                } else if (!silent) {
                    // Keep local; maybe first use
                    val local = cloudStore.snapshot()
                    _ui.update {
                        it.copy(
                            snackbar = if (local.isEmpty())
                                "Nenhum índice na nuvem ainda — faça um backup"
                            else
                                "Usando galeria local (${local.size})"
                        )
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _ui.update { it.copy(snackbar = "Falha ao sincronizar nuvem: ${e.message}") }
                }
            } finally {
                _ui.update { it.copy(isSyncingCloud = false) }
            }
        }
    }

    private fun publishCloudIndex() {
        val cfg = settings.value
        if (cfg.botToken.isBlank() || cfg.chatId.isBlank()) return
        viewModelScope.launch {
            try {
                val snapshot = cloudStore.snapshot()
                if (snapshot.isEmpty()) return@launch
                val published = cloudIndex.publishIndex(cfg.botToken, cfg.chatId, snapshot)
                if (published != null) {
                    prefs.setCloudIndexMeta(published.messageId, published.fileId)
                }
            } catch (_: Exception) {
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
        _ui.update {
            it.copy(
                cloudViewer = item,
                cloudFileUrl = it.cloudUrlByFileId[item.fileId] ?: item.localUri.takeIf { u -> u.isNotBlank() },
                isLoadingCloudUrl = item.fileId !in it.cloudUrlByFileId && item.localUri.isBlank()
            )
        }
        // Resolve current + neighbors for fast swipe
        ensureCloudUrlsAround(item)
    }

    fun onCloudPageChanged(item: CloudMediaItem) {
        _ui.update { it.copy(cloudViewer = item) }
        ensureCloudUrlsAround(item)
    }

    private fun ensureCloudUrlsAround(center: CloudMediaItem) {
        val all = cloudStore.items.value
        val idx = all.indexOfFirst { it.id == center.id }.coerceAtLeast(0)
        val window = all.subList(
            (idx - 1).coerceAtLeast(0),
            (idx + 3).coerceAtMost(all.size)
        )
        window.forEach { resolveCloudUrl(it) }
    }

    private fun resolveCloudUrl(item: CloudMediaItem) {
        // already have full url
        if (item.fileId.isNotBlank() && item.fileId in _ui.value.cloudUrlByFileId) return
        // local uri available
        if (item.localUri.isNotBlank()) {
            _ui.update { state ->
                val map = state.cloudUrlByFileId.toMutableMap()
                if (item.fileId.isNotBlank()) map[item.fileId] = item.localUri
                state.copy(
                    cloudUrlByFileId = map,
                    isLoadingCloudUrl = if (state.cloudViewer?.id == item.id) false else state.isLoadingCloudUrl,
                    cloudFileUrl = if (state.cloudViewer?.id == item.id) item.localUri else state.cloudFileUrl
                )
            }
            // still try telegram url in background for better quality if needed
        }

        val token = settings.value.botToken
        if (token.isBlank() || item.fileId.isBlank()) {
            if (item.localUri.isBlank()) {
                _ui.update {
                    it.copy(
                        isLoadingCloudUrl = if (it.cloudViewer?.id == item.id) false else it.isLoadingCloudUrl
                    )
                }
            }
            return
        }

        _ui.update { it.copy(cloudLoadingIds = it.cloudLoadingIds + item.fileId) }
        viewModelScope.launch {
            try {
                val fullUrl = uploader.getFileUrl(token, item.fileId)
                val thumbUrl = if (item.thumbFileId.isNotBlank()) {
                    uploader.getFileUrl(token, item.thumbFileId)
                } else null
                val chosen = fullUrl ?: thumbUrl ?: item.localUri.takeIf { it.isNotBlank() }
                _ui.update { state ->
                    val map = state.cloudUrlByFileId.toMutableMap()
                    if (fullUrl != null) map[item.fileId] = fullUrl
                    if (thumbUrl != null && item.thumbFileId.isNotBlank()) {
                        map[item.thumbFileId] = thumbUrl
                    }
                    // fallback map by fileId to local if no remote
                    if (fullUrl == null && chosen != null) map[item.fileId] = chosen
                    state.copy(
                        cloudUrlByFileId = map,
                        cloudLoadingIds = state.cloudLoadingIds - item.fileId,
                        isLoadingCloudUrl = if (state.cloudViewer?.id == item.id) false else state.isLoadingCloudUrl,
                        cloudFileUrl = if (state.cloudViewer?.id == item.id) {
                            map[item.fileId] ?: chosen ?: state.cloudFileUrl
                        } else state.cloudFileUrl
                    )
                }
            } catch (_: Exception) {
                _ui.update { state ->
                    state.copy(
                        cloudLoadingIds = state.cloudLoadingIds - item.fileId,
                        isLoadingCloudUrl = if (state.cloudViewer?.id == item.id) false else state.isLoadingCloudUrl,
                        cloudFileUrl = if (state.cloudViewer?.id == item.id) {
                            item.localUri.takeIf { it.isNotBlank() } ?: state.cloudFileUrl
                        } else state.cloudFileUrl
                    )
                }
            }
        }
    }

    fun closeCloudViewer() {
        _ui.update {
            it.copy(
                cloudViewer = null,
                cloudFileUrl = null,
                isLoadingCloudUrl = false
            )
        }
    }

    fun removeCloudItem(id: String) {
        viewModelScope.launch {
            cloudStore.remove(id)
            publishCloudIndex()
            _ui.update { it.copy(snackbar = "Removido da galeria na nuvem") }
        }
    }

    fun clearCloudGallery() {
        viewModelScope.launch {
            cloudStore.clear()
            publishCloudIndex()
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
        if (BackupRuntime.isRunning.value || _ui.value.backup.state == BackupState.Uploading) {
            _ui.update { it.copy(snackbar = "Já existe um backup em andamento") }
            return
        }

        BackupRuntime.pendingJob = BackupRuntime.Job(
            token = cfg.botToken,
            chatId = cfg.chatId,
            items = selected,
            options = cfg.metadata
        )

        refreshBatteryStatus()
        val stillOptimized = !BatteryOptimization.isIgnoringBatteryOptimizations(appContext)
        if (stillOptimized) {
            _ui.update { it.copy(showBatteryDialog = true, batteryOptimized = true) }
            return
        }
        launchPendingBackup()
    }

    fun continueBackupAfterBatteryPrompt() {
        _ui.update { it.copy(showBatteryDialog = false) }
        launchPendingBackup()
    }

    private fun launchPendingBackup() {
        val pending = BackupRuntime.pendingJob
        if (pending == null) {
            _ui.update { it.copy(snackbar = "Nada para enviar") }
            return
        }
        if (BackupRuntime.isRunning.value) return

        _ui.update {
            it.copy(
                backup = BackupProgress(
                    state = BackupState.Uploading,
                    current = 0,
                    total = pending.items.size,
                    message = "Iniciando serviço de backup…"
                ),
                snackbar = "Backup em segundo plano · veja a notificação"
            )
        }
        try {
            BackupService.start(appContext)
        } catch (e: Exception) {
            BackupRuntime.pendingJob = null
            _ui.update {
                it.copy(
                    backup = BackupProgress(
                        state = BackupState.Error,
                        message = "Falha ao iniciar serviço: ${e.message}"
                    ),
                    snackbar = "Não foi possível iniciar o backup em segundo plano"
                )
            }
        }
    }

    fun clearSnackbar() {
        _ui.update { it.copy(snackbar = null) }
    }

    fun resetBackupState() {
        if (!BackupRuntime.isRunning.value) {
            BackupRuntime.resetIfIdle()
            _ui.update { it.copy(backup = BackupProgress()) }
        }
    }

    // ── In-app updater ──────────────────────────────────────────────

    fun checkForAppUpdate(silent: Boolean = false) {
        if (_ui.value.update.phase == UpdatePhase.Downloading ||
            _ui.value.update.phase == UpdatePhase.Checking
        ) return

        viewModelScope.launch {
            _ui.update {
                it.copy(
                    update = it.update.copy(
                        phase = UpdatePhase.Checking,
                        visible = !silent,
                        message = "Consultando GitHub…",
                        progress = 0f
                    )
                )
            }
            val result = updateManager.checkForUpdate()
            result.fold(
                onSuccess = { info ->
                    if (info == null) {
                        _ui.update {
                            it.copy(
                                update = if (silent) UpdateUiState()
                                else UpdateUiState(
                                    phase = UpdatePhase.UpToDate,
                                    visible = true,
                                    message = "Você já está na versão ${updateManager.currentVersionName()}"
                                ),
                                snackbar = if (silent) it.snackbar
                                else "App atualizado (v${updateManager.currentVersionName()})"
                            )
                        }
                        if (!silent) {
                            // auto-hide "up to date" banner
                            kotlinx.coroutines.delay(2500)
                            dismissUpdateBanner()
                        }
                    } else {
                        _ui.update {
                            it.copy(
                                update = UpdateUiState(
                                    phase = UpdatePhase.Available,
                                    info = info,
                                    visible = true,
                                    message = "Sua: v${updateManager.currentVersionName()} → Nova: ${info.tag}"
                                )
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _ui.update {
                        it.copy(
                            update = if (silent) UpdateUiState()
                            else UpdateUiState(
                                phase = UpdatePhase.Error,
                                visible = true,
                                message = e.message ?: "Erro ao verificar"
                            )
                        )
                    }
                }
            )
        }
    }

    fun startUpdateDownload() {
        val info = _ui.value.update.info ?: return
        if (_ui.value.update.phase == UpdatePhase.Downloading) return
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    update = it.update.copy(
                        phase = UpdatePhase.Downloading,
                        visible = true,
                        progress = 0f,
                        downloadedBytes = 0,
                        totalBytes = info.sizeBytes,
                        message = "Baixando ${info.apkName}…"
                    )
                )
            }
            val result = updateManager.downloadApk(info) { downloaded, total ->
                val progress = if (total > 0) downloaded.toFloat() / total else 0f
                _ui.update { state ->
                    state.copy(
                        update = state.update.copy(
                            phase = UpdatePhase.Downloading,
                            progress = progress.coerceIn(0f, 1f),
                            downloadedBytes = downloaded,
                            totalBytes = if (total > 0) total else state.update.totalBytes,
                            message = "Baixando atualização…"
                        )
                    )
                }
            }
            result.fold(
                onSuccess = { file ->
                    downloadedApk = file
                    _ui.update {
                        it.copy(
                            update = it.update.copy(
                                phase = UpdatePhase.ReadyToInstall,
                                progress = 1f,
                                message = "Download concluído · ${info.tag}",
                                visible = true
                            )
                        )
                    }
                },
                onFailure = { e ->
                    _ui.update {
                        it.copy(
                            update = it.update.copy(
                                phase = UpdatePhase.Error,
                                message = e.message ?: "Falha no download",
                                visible = true
                            )
                        )
                    }
                }
            )
        }
    }

    fun installDownloadedUpdate(activity: android.app.Activity) {
        val file = downloadedApk
        if (file == null || !file.exists()) {
            _ui.update {
                it.copy(
                    update = it.update.copy(
                        phase = UpdatePhase.Error,
                        message = "Arquivo não encontrado — baixe de novo",
                        visible = true
                    )
                )
            }
            return
        }
        if (!updateManager.canInstallPackages()) {
            _ui.update {
                it.copy(
                    snackbar = "Permita instalar apps desconhecidos para o TeleBackup",
                    update = it.update.copy(
                        phase = UpdatePhase.ReadyToInstall,
                        message = "Autorize instalação e toque em Instalar"
                    )
                )
            }
            updateManager.openUnknownSourcesSettings(activity)
            return
        }
        _ui.update {
            it.copy(
                update = it.update.copy(
                    phase = UpdatePhase.Installing,
                    message = "Abrindo instalador…"
                )
            )
        }
        val ok = updateManager.installApk(file, activity)
        if (!ok) {
            _ui.update {
                it.copy(
                    update = it.update.copy(
                        phase = UpdatePhase.Error,
                        message = "Não foi possível abrir o instalador"
                    )
                )
            }
        }
    }

    fun dismissUpdateBanner() {
        if (_ui.value.update.phase == UpdatePhase.Downloading) return
        _ui.update { it.copy(update = UpdateUiState()) }
    }
}
