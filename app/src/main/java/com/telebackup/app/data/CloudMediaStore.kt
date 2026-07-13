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

/**
 * Local index of media already uploaded to Telegram (cloud gallery).
 * Persists as JSON so the user can browse everything sent by the app.
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
        // avoid duplicates by fileId
        current.removeAll { it.fileId == item.fileId && item.fileId.isNotBlank() }
        current.add(0, item)
        writeAll(current)
        _items.value = current
    }

    suspend fun addAll(newItems: List<CloudMediaItem>) = withContext(Dispatchers.IO) {
        if (newItems.isEmpty()) return@withContext
        val current = readAll().toMutableList()
        val existingIds = current.map { it.fileId }.toHashSet()
        for (item in newItems) {
            if (item.fileId.isNotBlank() && item.fileId in existingIds) continue
            current.add(0, item)
            if (item.fileId.isNotBlank()) existingIds += item.fileId
        }
        writeAll(current)
        _items.value = current
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
            }
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
}
