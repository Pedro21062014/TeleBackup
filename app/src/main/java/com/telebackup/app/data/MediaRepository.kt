package com.telebackup.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    /**
     * Loads photos + videos from primary external storage
     * (/storage/emulated/0 — DCIM, Pictures, Download, Movies, etc.)
     * via MediaStore, which is the correct Android API for that volume.
     */
    suspend fun loadDeviceMedia(limit: Int = 2000): List<MediaItem> = withContext(Dispatchers.IO) {
        coroutineScope {
            val imagesDef = async { queryImages(limit) }
            val videosDef = async { queryVideos(limit) }
            val images = imagesDef.await()
            val videos = videosDef.await()
            (images + videos)
                .sortedByDescending { it.dateAdded }
                .take(limit)
        }
    }

    private fun imageCollection(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun videoCollection(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun queryImages(limit: Int): List<MediaItem> {
        val items = ArrayList<MediaItem>(minOf(limit, 512))
        val collection = imageCollection()

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Images.Media.RELATIVE_PATH
            projection += MediaStore.Images.Media.IS_PENDING
        } else {
            @Suppress("DEPRECATION")
            projection += MediaStore.Images.Media.DATA
        }

        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        // Only fully written files on Q+
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.IS_PENDING}=0 AND ${MediaStore.Images.Media.SIZE}>0"
        } else {
            "${MediaStore.Images.Media.SIZE}>0"
        }

        try {
            context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                selection,
                null,
                sort
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val modCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else -1
                @Suppress("DEPRECATION")
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                } else -1

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    // Prefer primary emulated storage content
                    if (!isPrimaryStorage(cursor, relCol, dataCol)) continue

                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val dateAdded = cursor.getLong(dateCol).let { d ->
                        if (d > 0) d else if (modCol >= 0) cursor.getLong(modCol) else 0L
                    }
                    val bucket = when {
                        bucketCol >= 0 && !cursor.isNull(bucketCol) ->
                            cursor.getString(bucketCol)
                        relCol >= 0 && !cursor.isNull(relCol) ->
                            cursor.getString(relCol)?.trimEnd('/')?.substringAfterLast('/')
                        else -> "Pictures"
                    }
                    items += MediaItem(
                        id = id,
                        uri = uri,
                        name = cursor.getString(nameCol) ?: "foto_$id.jpg",
                        mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                        size = cursor.getLong(sizeCol),
                        dateAdded = dateAdded,
                        folderName = bucket ?: "Pictures"
                    )
                    count++
                }
            }
        } catch (_: SecurityException) {
            // missing permission
        } catch (_: Exception) {
        }
        return items
    }

    private fun queryVideos(limit: Int): List<MediaItem> {
        val items = ArrayList<MediaItem>(minOf(limit, 256))
        val collection = videoCollection()

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Video.Media.RELATIVE_PATH
            projection += MediaStore.Video.Media.IS_PENDING
        } else {
            @Suppress("DEPRECATION")
            projection += MediaStore.Video.Media.DATA
        }

        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.IS_PENDING}=0 AND ${MediaStore.Video.Media.SIZE}>0"
        } else {
            "${MediaStore.Video.Media.SIZE}>0"
        }

        try {
            context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                selection,
                null,
                sort
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val modCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else -1
                @Suppress("DEPRECATION")
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                } else -1

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    if (!isPrimaryStorage(cursor, relCol, dataCol)) continue

                    val id = cursor.getLong(idCol)
                    val syntheticId = id + VIDEO_ID_OFFSET
                    val uri = ContentUris.withAppendedId(collection, id)
                    val dateAdded = cursor.getLong(dateCol).let { d ->
                        if (d > 0) d else if (modCol >= 0) cursor.getLong(modCol) else 0L
                    }
                    val bucket = when {
                        bucketCol >= 0 && !cursor.isNull(bucketCol) ->
                            cursor.getString(bucketCol)
                        relCol >= 0 && !cursor.isNull(relCol) ->
                            cursor.getString(relCol)?.trimEnd('/')?.substringAfterLast('/')
                        else -> "Movies"
                    }
                    items += MediaItem(
                        id = syntheticId,
                        uri = uri,
                        name = cursor.getString(nameCol) ?: "video_$id.mp4",
                        mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                        size = cursor.getLong(sizeCol),
                        dateAdded = dateAdded,
                        folderName = bucket ?: "Movies",
                        durationMs = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    )
                    count++
                }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
        return items
    }

    /**
     * Accept media on primary external volume (/storage/emulated/0/...).
     * On Android 10+, VOLUME_EXTERNAL already targets that; still filter
     * obvious secondary/USB paths when DATA is available.
     */
    private fun isPrimaryStorage(
        cursor: android.database.Cursor,
        relCol: Int,
        dataCol: Int
    ): Boolean {
        if (dataCol >= 0 && !cursor.isNull(dataCol)) {
            val path = cursor.getString(dataCol) ?: return true
            // Keep primary emulated storage and common roots
            if (path.startsWith("/storage/emulated/0") ||
                path.startsWith("/sdcard") ||
                path.startsWith("/storage/self/primary")
            ) return true
            // Drop secondary volumes like /storage/XXXX-XXXX
            if (path.startsWith("/storage/") && !path.startsWith("/storage/emulated")) {
                return false
            }
            // Absolute path under Environment.getExternalStorageDirectory()
            val primary = Environment.getExternalStorageDirectory()?.absolutePath
            if (primary != null && path.startsWith(primary)) return true
        }
        // RELATIVE_PATH on Q+ is relative to primary volume when using VOLUME_EXTERNAL
        if (relCol >= 0) return true
        return true
    }

    suspend fun loadFromFolders(folderUris: Set<String>): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        var syntheticId = FOLDER_ID_OFFSET
        for (uriStr in folderUris) {
            try {
                val treeUri = Uri.parse(uriStr)
                walkTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri), items, syntheticId).also {
                    syntheticId = it
                }
            } catch (_: Exception) {
            }
        }
        items.sortedByDescending { it.dateAdded }
    }

    private fun walkTree(
        treeUri: Uri,
        parentDocId: String,
        items: MutableList<MediaItem>,
        startId: Long,
        depth: Int = 0
    ): Long {
        if (depth > 5) return startId
        var nextId = startId
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        try {
            context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val dateCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol) ?: continue
                    val mime = cursor.getString(mimeCol) ?: continue
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        nextId = walkTree(treeUri, docId, items, nextId, depth + 1)
                        continue
                    }
                    if (!mime.startsWith("image/") && !mime.startsWith("video/")) continue
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val name = cursor.getString(nameCol) ?: "arquivo"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val date = if (dateCol >= 0) cursor.getLong(dateCol) / 1000 else 0L
                    items += MediaItem(
                        id = nextId++,
                        uri = docUri,
                        name = name,
                        mimeType = mime,
                        size = size,
                        dateAdded = date,
                        folderName = folderDisplayName(treeUri)
                    )
                }
            }
        } catch (_: Exception) {
        }
        return nextId
    }

    fun folderDisplayName(uri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            docId.substringAfterLast(':').ifBlank { "Pasta" }
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast(':') ?: "Pasta"
        }
    }

    fun takePersistablePermission(uri: Uri) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val VIDEO_ID_OFFSET = 5_000_000_000L
        private const val FOLDER_ID_OFFSET = 1_000_000L
    }
}
