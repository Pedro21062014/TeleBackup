package com.telebackup.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.telebackup.app.MainActivity
import com.telebackup.app.R
import com.telebackup.app.data.BackupProgress
import com.telebackup.app.data.BackupState

object BackupNotifier {

    const val CHANNEL_PROGRESS = "telebackup_progress"
    const val CHANNEL_COMPLETE = "telebackup_complete"

    const val NOTIF_PROGRESS_ID = 2101
    const val NOTIF_COMPLETE_ID = 2102

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val progress = NotificationChannel(
            CHANNEL_PROGRESS,
            "Progresso do backup",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra o andamento do envio de fotos e vídeos"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val complete = NotificationChannel(
            CHANNEL_COMPLETE,
            "Backup concluído",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisa quando o backup termina"
            setShowBadge(true)
        }

        nm.createNotificationChannel(progress)
        nm.createNotificationChannel(complete)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "backup")
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun progressNotification(context: Context, progress: BackupProgress): Notification {
        ensureChannels(context)
        val max = progress.total.coerceAtLeast(1)
        val current = progress.current.coerceIn(0, max)
        val indeterminate = progress.total <= 0 || progress.state == BackupState.Scanning

        val title = when (progress.state) {
            BackupState.Uploading, BackupState.Scanning -> "Enviando backup…"
            BackupState.Success -> "Backup concluído"
            BackupState.Error -> "Erro no backup"
            BackupState.Idle -> "TeleBackup"
        }

        val text = buildString {
            if (progress.message.isNotBlank()) append(progress.message)
            else append("Preparando…")
            if (progress.currentFile.isNotBlank()) {
                append(" · ")
                append(progress.currentFile.take(40))
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_backup)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setOnlyAlertOnce(true)
            .setOngoing(progress.state == BackupState.Uploading || progress.state == BackupState.Scanning)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setColor(0xFF2AABEE.toInt())

        if (indeterminate) {
            builder.setProgress(0, 0, true)
        } else {
            builder.setProgress(max, current, false)
            builder.setSubText("$current/$max")
        }

        return builder.build()
    }

    fun updateProgress(context: Context, progress: BackupProgress) {
        ensureChannels(context)
        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIF_PROGRESS_ID, progressNotification(context, progress))
        } catch (_: SecurityException) {
            // missing POST_NOTIFICATIONS
        }
    }

    fun showCompleted(context: Context, success: Boolean, message: String) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(R.drawable.ic_stat_backup)
            .setContentTitle(if (success) "Backup concluído ✅" else "Backup com erros")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(0xFF2AABEE.toInt())
            .build()
        try {
            NotificationManagerCompat.from(context).cancel(NOTIF_PROGRESS_ID)
            NotificationManagerCompat.from(context).notify(NOTIF_COMPLETE_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelProgress(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_PROGRESS_ID)
    }
}
