package com.telebackup.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class TelegramUploader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    data class TestResult(val ok: Boolean, val message: String)

    data class UploadResult(
        val ok: Boolean,
        val cloudItem: CloudMediaItem? = null,
        val error: String? = null
    )

    suspend fun testConnection(token: String, chatId: String): TestResult = withContext(Dispatchers.IO) {
        try {
            val meReq = Request.Builder()
                .url("https://api.telegram.org/bot$token/getMe")
                .get()
                .build()
            client.newCall(meReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) {
                    return@withContext TestResult(false, "Token inválido: ${json.optString("description")}")
                }
                val botName = json.getJSONObject("result").optString("username", "bot")
                val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart(
                        "text",
                        "✅ TeleBackup conectado!\nBot: @$botName\nPronto para fotos e vídeos."
                    )
                    .build()
                val msgReq = Request.Builder()
                    .url("https://api.telegram.org/bot$token/sendMessage")
                    .post(form)
                    .build()
                client.newCall(msgReq).execute().use { msgResp ->
                    val msgBody = msgResp.body?.string().orEmpty()
                    val msgJson = JSONObject(msgBody)
                    if (msgJson.optBoolean("ok")) {
                        TestResult(true, "Conectado como @$botName")
                    } else {
                        TestResult(
                            false,
                            "Chat ID inválido ou bot sem acesso: ${msgJson.optString("description")}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            TestResult(false, "Erro de rede: ${e.message}")
        }
    }

    suspend fun uploadMedia(
        token: String,
        chatId: String,
        item: MediaItem,
        options: MetadataOptions
    ): UploadResult = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val exif = if (item.isImage) MetadataHelper.readExif(context, item.uri) else ExifInfo(
                latitude = item.latitude,
                longitude = item.longitude
            )
            val caption = MetadataHelper.buildCaption(item, exif, options)
            tempFile = MetadataHelper.prepareUploadFile(context, item, options)
            val bytesSize = tempFile.length()
            // Telegram limits: photo ~10MB, video/doc 50MB (bots)
            val isPhoto = item.isImage && bytesSize <= 10L * 1024 * 1024
            val isVideo = item.isVideo && bytesSize <= 50L * 1024 * 1024

            val (endpoint, field) = when {
                isPhoto -> "sendPhoto" to "photo"
                isVideo -> "sendVideo" to "video"
                else -> "sendDocument" to "document"
            }

            val mediaType = (item.mimeType.ifBlank { "application/octet-stream" }).toMediaTypeOrNull()
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart(field, item.name, tempFile.asRequestBody(mediaType))
                .addFormDataPart("caption", caption)
            if (isVideo) {
                builder.addFormDataPart("supports_streaming", "true")
                if (item.durationMs > 0) {
                    builder.addFormDataPart("duration", (item.durationMs / 1000).toString())
                }
            }

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/$endpoint")
                .post(builder.build())
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) {
                    // fallback document for failed photo/video
                    if (endpoint != "sendDocument") {
                        return@withContext uploadAsDocument(token, chatId, item, tempFile, caption, exif)
                    }
                    return@withContext UploadResult(false, error = json.optString("description", "Falha no envio"))
                }
                val result = json.getJSONObject("result")
                val cloud = parseCloudItem(result, item, caption, exif, bytesSize)
                UploadResult(true, cloud)
            }
        } catch (e: Exception) {
            UploadResult(false, error = e.message)
        } finally {
            tempFile?.delete()
        }
    }

    private fun uploadAsDocument(
        token: String,
        chatId: String,
        item: MediaItem,
        file: File,
        caption: String,
        exif: ExifInfo
    ): UploadResult {
        return try {
            val mediaType = (item.mimeType.ifBlank { "application/octet-stream" }).toMediaTypeOrNull()
            val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("document", item.name, file.asRequestBody(mediaType))
                .addFormDataPart("caption", caption.take(1024))
                .build()
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendDocument")
                .post(form)
                .build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) {
                    UploadResult(false, error = json.optString("description", "Falha no envio"))
                } else {
                    val result = json.getJSONObject("result")
                    UploadResult(true, parseCloudItem(result, item, caption, exif, file.length()))
                }
            }
        } catch (e: Exception) {
            UploadResult(false, error = e.message)
        }
    }

    private fun parseCloudItem(
        result: JSONObject,
        item: MediaItem,
        caption: String,
        exif: ExifInfo,
        size: Long
    ): CloudMediaItem {
        val messageId = result.optLong("message_id")
        var fileId = ""
        var thumbId = ""
        var mime = item.mimeType
        var isVideo = item.isVideo

        when {
            result.has("photo") -> {
                val photos = result.getJSONArray("photo")
                val best = photos.getJSONObject(photos.length() - 1)
                fileId = best.optString("file_id")
                if (photos.length() > 0) {
                    thumbId = photos.getJSONObject(0).optString("file_id")
                }
                mime = "image/jpeg"
                isVideo = false
            }
            result.has("video") -> {
                val video = result.getJSONObject("video")
                fileId = video.optString("file_id")
                thumbId = video.optJSONObject("thumbnail")?.optString("file_id").orEmpty()
                mime = video.optString("mime_type", item.mimeType)
                isVideo = true
            }
            result.has("document") -> {
                val doc = result.getJSONObject("document")
                fileId = doc.optString("file_id")
                thumbId = doc.optJSONObject("thumbnail")?.optString("file_id").orEmpty()
                mime = doc.optString("mime_type", item.mimeType)
                isVideo = mime.startsWith("video")
            }
        }

        val lat = exif.latitude ?: item.latitude
        val lon = exif.longitude ?: item.longitude

        return CloudMediaItem(
            id = UUID.randomUUID().toString(),
            name = item.name,
            mimeType = mime,
            size = size,
            isVideo = isVideo,
            fileId = fileId,
            thumbFileId = thumbId,
            messageId = messageId,
            uploadedAt = System.currentTimeMillis(),
            caption = caption,
            localUri = item.uri.toString(),
            hasLocation = lat != null && lon != null,
            latitude = lat,
            longitude = lon
        )
    }

    suspend fun backupAll(
        token: String,
        chatId: String,
        items: List<MediaItem>,
        options: MetadataOptions,
        onProgress: (BackupProgress) -> Unit,
        onUploaded: suspend (CloudMediaItem) -> Unit
    ): BackupProgress {
        if (items.isEmpty()) {
            val p = BackupProgress(BackupState.Error, message = "Nenhuma mídia encontrada")
            onProgress(p)
            return p
        }
        val uploaded = mutableSetOf<Long>()
        onProgress(
            BackupProgress(
                state = BackupState.Uploading,
                current = 0,
                total = items.size,
                message = "Iniciando backup..."
            )
        )
        try {
            sendText(
                token, chatId,
                "🚀 *Backup iniciado*\nTotal: ${items.size} arquivo(s)\nFotos + vídeos · TeleBackup"
            )
        } catch (_: Exception) {
        }

        for ((index, item) in items.withIndex()) {
            onProgress(
                BackupProgress(
                    state = BackupState.Uploading,
                    current = index,
                    total = items.size,
                    currentFile = item.name,
                    message = "Enviando ${index + 1}/${items.size}",
                    uploadedIds = uploaded.toSet()
                )
            )
            val result = uploadMedia(token, chatId, item, options)
            if (result.ok && result.cloudItem != null) {
                uploaded += item.id
                onUploaded(result.cloudItem)
            } else {
                onProgress(
                    BackupProgress(
                        state = BackupState.Uploading,
                        current = index + 1,
                        total = items.size,
                        currentFile = item.name,
                        message = "Erro em ${item.name}: ${result.error}",
                        uploadedIds = uploaded.toSet()
                    )
                )
            }
            delay(400)
        }

        val final = BackupProgress(
            state = BackupState.Success,
            current = items.size,
            total = items.size,
            message = "Backup concluído: ${uploaded.size}/${items.size} enviados",
            uploadedIds = uploaded.toSet()
        )
        onProgress(final)
        try {
            sendText(
                token, chatId,
                "✅ *Backup concluído*\nEnviados: ${uploaded.size}/${items.size}"
            )
        } catch (_: Exception) {
        }
        return final
    }

    /** Resolve Telegram file_id to a temporary download URL */
    suspend fun getFileUrl(token: String, fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$token/getFile?file_id=$fileId")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                if (!json.optBoolean("ok")) return@withContext null
                val path = json.getJSONObject("result").optString("file_path")
                if (path.isBlank()) null else "https://api.telegram.org/file/bot$token/$path"
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sendText(token: String, chatId: String, text: String) {
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("text", text)
            .addFormDataPart("parse_mode", "Markdown")
            .build()
        val request = Request.Builder()
            .url("https://api.telegram.org/bot$token/sendMessage")
            .post(form)
            .build()
        client.newCall(request).execute().close()
    }
}
