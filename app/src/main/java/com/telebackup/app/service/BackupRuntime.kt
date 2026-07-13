package com.telebackup.app.service

import com.telebackup.app.data.BackupProgress
import com.telebackup.app.data.BackupState
import com.telebackup.app.data.CloudMediaItem
import com.telebackup.app.data.MediaItem
import com.telebackup.app.data.MetadataOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory bridge between UI and the foreground BackupService.
 */
object BackupRuntime {

    data class Job(
        val token: String,
        val chatId: String,
        val items: List<MediaItem>,
        val options: MetadataOptions
    )

    @Volatile
    var pendingJob: Job? = null

    private val _progress = MutableStateFlow(BackupProgress())
    val progress: StateFlow<BackupProgress> = _progress.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _uploaded = MutableSharedFlow<CloudMediaItem>(extraBufferCapacity = 64)
    val uploaded: SharedFlow<CloudMediaItem> = _uploaded.asSharedFlow()

    private val _finished = MutableSharedFlow<BackupProgress>(extraBufferCapacity = 1)
    val finished: SharedFlow<BackupProgress> = _finished.asSharedFlow()

    fun setProgress(value: BackupProgress) {
        _progress.value = value
        _isRunning.value = value.state == BackupState.Uploading || value.state == BackupState.Scanning
    }

    fun emitUploaded(item: CloudMediaItem) {
        _uploaded.tryEmit(item)
    }

    fun emitFinished(value: BackupProgress) {
        _progress.value = value
        _isRunning.value = false
        _finished.tryEmit(value)
    }

    fun resetIfIdle() {
        if (!_isRunning.value && _progress.value.state != BackupState.Uploading) {
            _progress.value = BackupProgress()
        }
    }
}
