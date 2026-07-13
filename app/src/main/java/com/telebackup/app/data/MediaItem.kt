package com.telebackup.app.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val folderName: String = "",
    val durationMs: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val isVideo: Boolean get() = mimeType.startsWith("video")
    val isImage: Boolean get() = mimeType.startsWith("image")

    val sizeLabel: String
        get() = formatSize(size)

    val durationLabel: String
        get() {
            if (durationMs <= 0) return ""
            val totalSec = durationMs / 1000
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(m, s)
        }

    companion object {
        fun formatSize(size: Long): String = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
            size < 1024L * 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024))
            else -> "%.2f GB".format(size / (1024.0 * 1024 * 1024))
        }
    }
}

enum class BackupState {
    Idle, Scanning, Uploading, Success, Error
}

data class BackupProgress(
    val state: BackupState = BackupState.Idle,
    val current: Int = 0,
    val total: Int = 0,
    val currentFile: String = "",
    val message: String = "",
    val uploadedIds: Set<Long> = emptySet()
)

/** Options for what metadata goes with the upload */
data class MetadataOptions(
    val keepOriginalFile: Boolean = true,
    val stripLocation: Boolean = false,
    val stripCameraInfo: Boolean = false,
    val stripAllExif: Boolean = false,
    val includeLocationInCaption: Boolean = false,
    val includeDateInCaption: Boolean = true,
    val includeFileNameInCaption: Boolean = true,
    val includeFolderInCaption: Boolean = true,
    val includeSizeInCaption: Boolean = true,
    val includeCameraInCaption: Boolean = false
)

/** Item already backed up / visible in cloud gallery */
data class CloudMediaItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val isVideo: Boolean,
    val fileId: String,
    val thumbFileId: String = "",
    val messageId: Long = 0,
    val uploadedAt: Long = System.currentTimeMillis(),
    val caption: String = "",
    val localUri: String = "",
    val hasLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val sizeLabel: String get() = MediaItem.formatSize(size)
}
