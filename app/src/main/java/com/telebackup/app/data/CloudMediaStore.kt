package com.telebackup.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local index of media already uploaded to Telegram (cloud gallery).
 * Also synced as a pinned document in the chat so reinstall + same credentials restores it.
 */
class CloudMediaStore(private val context: Context) {

    private val file: File
        get() = File(context.filesDir, "cloud_gallery.json")

    private val _items = MutableStateFlow<List<CloudMediaItem>>(emptyList())
    val items: StateFlow<List<CloudMediaItem>> = _items.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _items.value = readAll()
    }

    suspend fun add(item: CloudMediaItem) = withContext(Dispatchers.IO) {
        val current = readAll().toMutableList()
        current.removeAll { it.fileId == item.fileId && item.fileId.isNotBlank() }
        current.add(0, item)
        val sorted = current.sortedByDescending { it.uploadedAt }
        writeAll(sorted)
        _items.value = sorted
    }

    suspend fun addAll(newItems: List<CloudMediaItem>, replace: Boolean = false) = withContext(Dispatchers.IO) {
        if (newItems.isEmpty() && !replace) return@withContext
        val merged = if (replace) {
            newItems.sortedByDescending { it.uploadedAt }
        } else {
            val current = readAll().toMutableList()
            val existingIds = current.map { it.fileId }.filter { it.isNotBlank() }.toHashSet()
            for (item in newItems) {
                if (item.fileId.isNotBlank() && item.fileId in existingIds) {
                    // keep newer metadata if same fileId
                    val idx = current.indexOfFirst { it.fileId == item.fileId }
                    if (idx >= 0 && item.uploadedAt >= current[idx].uploadedAt) {
                        current[idx] = item
                    }
                    continue
                }
                current.add(item)
                if (item.fileId.isNotBlank()) existingIds += item.fileId
            }
            current.sortedByDescending { it.uploadedAt }
        }
        writeAll(merged)
        _items.value = merged
    }

    suspend fun snapshot(): List<CloudMediaItem> = withContext(Dispatchers.IO) {
        readAll()
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        val current = readAll().filterNot { it.id == id }
        writeAll(current)
        _items.value = current
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        writeAll(emptyList())
        _items.value = emptyList()
    }

    private fun readAll(): List<CloudMediaItem> {
        return try {
            if (!file.exists()) return emptyList()
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        CloudMediaItem(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            mimeType = o.optString("mimeType"),
                            size = o.optLong("size"),
                            isVideo = o.optBoolean("isVideo"),
                            fileId = o.optString("fileId"),
                            thumbFileId = o.optString("thumbFileId"),
                            messageId = o.optLong("messageId"),
                            uploadedAt = o.optLong("uploadedAt"),
                            caption = o.optString("caption"),
                            localUri = o.optString("localUri"),
                            hasLocation = o.optBoolean("hasLocation"),
                            latitude = o.optDouble("latitude").takeIf { o.has("latitude") && !o.isNull("latitude") },
                            longitude = o.optDouble("longitude").takeIf { o.has("longitude") && !o.isNull("longitude") }
                        )
                    )
                }
            }.sortedByDescending { it.uploadedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeAll(items: List<CloudMediaItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("mimeType", item.mimeType)
                    put("size", item.size)
                    put("isVideo", item.isVideo)
                    put("fileId", item.fileId)
                    put("thumbFileId", item.thumbFileId)
                    put("messageId", item.messageId)
                    put("uploadedAt", item.uploadedAt)
                    put("caption", item.caption)
                    put("localUri", item.localUri)
                    put("hasLocation", item.hasLocation)
                    if (item.latitude != null) put("latitude", item.latitude) else put("latitude", JSONObject.NULL)
                    if (item.longitude != null) put("longitude", item.longitude) else put("longitude", JSONObject.NULL)
                }
            )
        }
        file.writeText(arr.toString())
    }

    companion object {
        fun formatDate(ts: Long): String {
            if (ts <= 0) return "—"
            return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))
        }

        fun formatDateShort(ts: Long): String {
            if (ts <= 0) return ""
            return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ts))
        }
    }
}
