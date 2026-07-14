package com.telebackup.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Remote cloud gallery index stored as a **pinned document** in the Telegram chat.
 *
 * After uninstall/reinstall, re-entering the same bot token + chat id restores
 * the gallery via getChat → pinned_message (caption #TeleBackupCloudIndex).
 */
class CloudIndexRemote(private val context: Context) {

    companion object {
        const val INDEX_CAPTION = "#TeleBackupCloudIndex"
        const val INDEX_FILE_NAME = "telebackup_cloud_index.json"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class PublishResult(val messageId: Long, val fileId: String)
    data class FetchResult(val items: List<CloudMediaItem>, val messageId: Long, val fileId: String)

    fun itemsToJson(items: List<CloudMediaItem>): String {
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
                    put("hasLocation", item.hasLocation)
                    if (item.latitude != null) put("latitude", item.latitude)
                    if (item.longitude != null) put("longitude", item.longitude)
                }
            )
        }
        return JSONObject()
            .put("version", 1)
            .put("updatedAt", System.currentTimeMillis())
            .put("items", arr)
            .toString()
    }

    fun parseIndexJson(text: String): List<CloudMediaItem> {
        return try {
            val root = JSONObject(text)
            val arr = root.optJSONArray("items") ?: JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val fileId = o.optString("fileId")
                    if (fileId.isBlank()) continue
                    add(
                        CloudMediaItem(
                            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = o.optString("name", "mídia"),
                            mimeType = o.optString("mimeType", "image/jpeg"),
                            size = o.optLong("size"),
                            isVideo = o.optBoolean("isVideo"),
                            fileId = fileId,
                            thumbFileId = o.optString("thumbFileId"),
                            messageId = o.optLong("messageId"),
                            uploadedAt = o.optLong("uploadedAt", System.currentTimeMillis()),
                            caption = o.optString("caption"),
                            localUri = "",
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

    /** Upload (or replace) index document and pin it. */
    suspend fun publishIndex(
        token: String,
        chatId: String,
        items: List<CloudMediaItem>
    ): PublishResult? = withContext(Dispatchers.IO) {
        try {
            val json = itemsToJson(items)
            val tmp = File(context.cacheDir, INDEX_FILE_NAME)
            tmp.writeText(json)

            val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart(
                    "document",
                    INDEX_FILE_NAME,
                    tmp.asRequestBody("application/json".toMediaTypeOrNull())
                )
                .addFormDataPart(
                    "caption",
                    "$INDEX_CAPTION\n📦 Índice da galeria TeleBackup\n${items.size} item(ns) · reabre após reinstalar"
                )
                .build()

            val req = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendDocument")
                .post(form)
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val jsonResp = JSONObject(body)
                if (!jsonResp.optBoolean("ok")) return@withContext null
                val result = jsonResp.getJSONObject("result")
                val messageId = result.optLong("message_id")
                val fileId = result.optJSONObject("document")?.optString("file_id").orEmpty()
                // Pin so getChat can recover after reinstall
                pinMessage(token, chatId, messageId)
                PublishResult(messageId, fileId)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun pinMessage(token: String, chatId: String, messageId: Long) {
        try {
            val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("message_id", messageId.toString())
                .addFormDataPart("disable_notification", "true")
                .build()
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$token/pinChatMessage")
                .post(form)
                .build()
            client.newCall(req).execute().close()
        } catch (_: Exception) {
        }
    }

    /**
     * Recover gallery after reinstall: read pinned message from getChat.
     * Falls back to known file_id if provided.
     */
    suspend fun fetchIndex(
        token: String,
        chatId: String,
        knownFileId: String = ""
    ): FetchResult? = withContext(Dispatchers.IO) {
        try {
            // 1) Prefer pinned message on the chat
            val chatReq = Request.Builder()
                .url("https://api.telegram.org/bot$token/getChat?chat_id=$chatId")
                .get()
                .build()
            var messageId = 0L
            var fileId = knownFileId

            client.newCall(chatReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                if (json.optBoolean("ok")) {
                    val chat = json.getJSONObject("result")
                    val pinned = chat.optJSONObject("pinned_message")
                    if (pinned != null) {
                        val caption = pinned.optString("caption") + " " + pinned.optString("text")
                        val doc = pinned.optJSONObject("document")
                        if (caption.contains("TeleBackupCloudIndex") && doc != null) {
                            messageId = pinned.optLong("message_id")
                            fileId = doc.optString("file_id")
                        }
                    }
                }
            }

            if (fileId.isBlank()) return@withContext null

            // 2) Download file
            val pathReq = Request.Builder()
                .url("https://api.telegram.org/bot$token/getFile?file_id=$fileId")
                .get()
                .build()
            val filePath = client.newCall(pathReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) return@withContext null
                json.getJSONObject("result").optString("file_path")
            }
            if (filePath.isBlank()) return@withContext null

            val downloadUrl = "https://api.telegram.org/file/bot$token/$filePath"
            val text = client.newCall(Request.Builder().url(downloadUrl).get().build())
                .execute().use { it.body?.string().orEmpty() }
            if (text.isBlank()) return@withContext null

            val items = parseIndexJson(text)
            FetchResult(items, messageId, fileId)
        } catch (_: Exception) {
            null
        }
    }
}
