package com.telebackup.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.telebackup.app.data.BackupState
import com.telebackup.app.data.CloudIndexRemote
import com.telebackup.app.data.CloudMediaStore
import com.telebackup.app.data.TelegramUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BackupService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        BackupNotifier.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopWork()
                return START_NOT_STICKY
            }
            ACTION_START, null -> startWork()
        }
        return START_NOT_STICKY
    }

    private fun startWork() {
        if (job?.isActive == true) return
        val pending = BackupRuntime.pendingJob
        if (pending == null || pending.items.isEmpty()) {
            stopSelf()
            return
        }

        val initial = com.telebackup.app.data.BackupProgress(
            state = BackupState.Uploading,
            current = 0,
            total = pending.items.size,
            message = "Iniciando backup…"
        )
        BackupRuntime.setProgress(initial)

        val notification = BackupNotifier.progressNotification(this, initial)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    BackupNotifier.NOTIF_PROGRESS_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(BackupNotifier.NOTIF_PROGRESS_ID, notification)
            }
        } catch (e: Exception) {
            // Fallback without type on older / restricted devices
            try {
                startForeground(BackupNotifier.NOTIF_PROGRESS_ID, notification)
            } catch (_: Exception) {
                BackupRuntime.emitFinished(
                    com.telebackup.app.data.BackupProgress(
                        state = BackupState.Error,
                        message = "Não foi possível iniciar o serviço: ${e.message}"
                    )
                )
                stopSelf()
                return
            }
        }

        acquireWakeLock()

        val uploader = TelegramUploader(applicationContext)
        val cloudStore = CloudMediaStore(applicationContext)
        val cloudIndex = CloudIndexRemote(applicationContext)
        val prefs = (application as? com.telebackup.app.TeleBackupApp)?.preferences

        job = scope.launch {
            try {
                val result = uploader.backupAll(
                    token = pending.token,
                    chatId = pending.chatId,
                    items = pending.items,
                    options = pending.options,
                    onProgress = { progress ->
                        BackupRuntime.setProgress(progress)
                        BackupNotifier.updateProgress(this@BackupService, progress)
                    },
                    onUploaded = { cloud ->
                        cloudStore.add(cloud)
                        BackupRuntime.emitUploaded(cloud)
                    }
                )
                // Publish remote index so reinstall + same credentials restores Cloud tab
                try {
                    val snapshot = cloudStore.snapshot()
                    val published = cloudIndex.publishIndex(pending.token, pending.chatId, snapshot)
                    if (published != null && prefs != null) {
                        prefs.setCloudIndexMeta(published.messageId, published.fileId)
                    }
                } catch (_: Exception) {
                }
                BackupRuntime.emitFinished(result)
                BackupNotifier.showCompleted(
                    this@BackupService,
                    success = result.state == BackupState.Success,
                    message = result.message.ifBlank {
                        if (result.state == BackupState.Success) "Backup concluído"
                        else "Backup finalizado com erros"
                    }
                )
            } catch (e: Exception) {
                val err = com.telebackup.app.data.BackupProgress(
                    state = BackupState.Error,
                    message = "Erro: ${e.message}"
                )
                BackupRuntime.emitFinished(err)
                BackupNotifier.showCompleted(this@BackupService, false, err.message)
            } finally {
                BackupRuntime.pendingJob = null
                releaseWakeLock()
                stopForegroundSafe()
                stopSelf()
            }
        }
    }

    private fun stopWork() {
        job?.cancel()
        job = null
        BackupRuntime.emitFinished(
            com.telebackup.app.data.BackupProgress(
                state = BackupState.Error,
                message = "Backup cancelado"
            )
        )
        BackupNotifier.cancelProgress(this)
        releaseWakeLock()
        stopForegroundSafe()
        stopSelf()
    }

    private fun stopForegroundSafe() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
        } catch (_: Exception) {
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TeleBackup::BackupWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(3 * 60 * 60 * 1000L) // max 3h
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    override fun onDestroy() {
        job?.cancel()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.telebackup.app.action.START_BACKUP"
        const val ACTION_STOP = "com.telebackup.app.action.STOP_BACKUP"

        fun start(context: Context) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
