package com.telebackup.app.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val tag: String,
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val apkName: String,
    val releaseNotes: String,
    val sizeBytes: Long
)

enum class UpdatePhase {
    Idle,
    Checking,
    Available,
    Downloading,
    ReadyToInstall,
    Installing,
    UpToDate,
    Error
}

data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.Idle,
    val info: UpdateInfo? = null,
    val progress: Float = 0f,          // 0..1
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val message: String = "",
    val visible: Boolean = false
)

class AppUpdateManager(private val context: Context) {

    companion object {
        private const val OWNER = "Pedro21062014"
        private const val REPO = "TeleBackup"
        private const val LATEST_URL =
            "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun currentVersionName(): String {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            p.versionName ?: "0"
        } catch (_: Exception) {
            "0"
        }
    }

    fun currentVersionCode(): Int {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) p.longVersionCode.toInt()
            else @Suppress("DEPRECATION") p.versionCode
        } catch (_: Exception) {
            0
        }
    }

    suspend fun checkForUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(LATEST_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "TeleBackup-Updater")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${resp.code}: ${resp.message}")
                    )
                }
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext Result.success(null)
                val json = JSONObject(body)
                val tag = json.optString("tag_name").removePrefix("v")
                val notes = json.optString("body").orEmpty()
                val assets = json.optJSONArray("assets")
                    ?: return@withContext Result.success(null)

                var apkUrl = ""
                var apkName = ""
                var size = 0L
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val name = a.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        // Prefer named signed release apk
                        val browser = a.optString("browser_download_url")
                        if (apkUrl.isBlank() ||
                            name.contains("signed", true) ||
                            name.contains(tag, true)
                        ) {
                            apkUrl = browser
                            apkName = name
                            size = a.optLong("size")
                        }
                    }
                }
                if (apkUrl.isBlank()) return@withContext Result.success(null)

                val remoteCode = parseVersionCode(tag)
                val localCode = currentVersionCode()
                val localName = currentVersionName()

                val isNewer = if (remoteCode > 0 && localCode > 0) {
                    remoteCode > localCode
                } else {
                    compareVersionNames(tag, localName) > 0
                }
                if (!isNewer) return@withContext Result.success(null)

                Result.success(
                    UpdateInfo(
                        tag = "v$tag",
                        versionName = tag,
                        versionCode = remoteCode,
                        apkUrl = apkUrl,
                        apkName = apkName.ifBlank { "TeleBackup-update.apk" },
                        releaseNotes = notes.take(800),
                        sizeBytes = size
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads APK to cache and reports progress 0..1
     */
    suspend fun downloadApk(
        info: UpdateInfo,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val out = File(dir, info.apkName.ifBlank { "TeleBackup-update.apk" })
            if (out.exists()) out.delete()

            val req = Request.Builder()
                .url(info.apkUrl)
                .header("User-Agent", "TeleBackup-Updater")
                .header("Accept", "application/octet-stream")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Download HTTP ${resp.code}"))
                }
                val body = resp.body ?: return@withContext Result.failure(Exception("Corpo vazio"))
                val total = when {
                    body.contentLength() > 0 -> body.contentLength()
                    info.sizeBytes > 0 -> info.sizeBytes
                    else -> -1L
                }
                body.byteStream().use { input ->
                    FileOutputStream(out).use { output ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        var downloaded = 0L
                        var lastEmit = 0L
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                            downloaded += read
                            // throttle UI updates a bit
                            if (downloaded - lastEmit > 128 * 1024 || downloaded == total) {
                                onProgress(downloaded, total)
                                lastEmit = downloaded
                            }
                        }
                        output.flush()
                        onProgress(downloaded, if (total > 0) total else downloaded)
                    }
                }
            }
            if (!out.exists() || out.length() < 1024) {
                return@withContext Result.failure(Exception("APK inválido após download"))
            }
            Result.success(out)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openUnknownSourcesSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
            activity.startActivity(intent)
        }
    }

    fun installApk(file: File, activity: Activity): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun parseVersionCode(versionName: String): Int {
        // Prefer explicit mapping from known tags; else synthesize from major.minor.patch
        // e.g. 1.5.1 -> 1*10000 + 5*100 + 1 = 10501
        val clean = versionName.trim().removePrefix("v")
        val parts = clean.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        return when {
            parts.isEmpty() -> 0
            parts.size == 1 -> parts[0]
            parts.size == 2 -> parts[0] * 100 + parts[1]
            else -> parts[0] * 10000 + parts[1] * 100 + parts[2]
        }
    }

    /** Returns >0 if a > b */
    private fun compareVersionNames(a: String, b: String): Int {
        val pa = a.removePrefix("v").split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        val pb = b.removePrefix("v").split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }
}
