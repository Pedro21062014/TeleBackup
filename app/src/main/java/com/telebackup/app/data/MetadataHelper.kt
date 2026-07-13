package com.telebackup.app.data

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExifInfo(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dateTime: String? = null,
    val make: String? = null,
    val model: String? = null,
    val software: String? = null,
    val orientation: String? = null
)

object MetadataHelper {

    fun readExif(context: Context, uri: Uri): ExifInfo {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)
                ExifInfo(
                    latitude = if (hasLatLong) latLong[0].toDouble() else null,
                    longitude = if (hasLatLong) latLong[1].toDouble() else null,
                    dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
                    make = exif.getAttribute(ExifInterface.TAG_MAKE),
                    model = exif.getAttribute(ExifInterface.TAG_MODEL),
                    software = exif.getAttribute(ExifInterface.TAG_SOFTWARE),
                    orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION)
                )
            } ?: ExifInfo()
        } catch (_: Exception) {
            ExifInfo()
        }
    }

    /**
     * Copies media to a temp file and optionally strips EXIF tags.
     * Returns the file to upload (caller should delete when done).
     */
    fun prepareUploadFile(
        context: Context,
        item: MediaItem,
        options: MetadataOptions
    ): File {
        val ext = item.name.substringAfterLast('.', if (item.isVideo) "mp4" else "jpg")
        val out = File(context.cacheDir, "upload_${System.currentTimeMillis()}_${item.id}.$ext")
        context.contentResolver.openInputStream(item.uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Não foi possível ler ${item.name}")

        val needsStrip = item.isImage && (
            options.stripAllExif || options.stripLocation || options.stripCameraInfo || !options.keepOriginalFile
            )
        if (needsStrip && out.exists() && out.length() > 0) {
            try {
                stripExif(out, options)
            } catch (_: Exception) {
                // keep copy even if strip fails
            }
        }
        return out
    }

    private fun stripExif(file: File, options: MetadataOptions) {
        val exif = ExifInterface(file.absolutePath)
        if (options.stripAllExif || !options.keepOriginalFile) {
            // Clear common privacy-sensitive tags
            val tags = listOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_IMAGE_DESCRIPTION,
                ExifInterface.TAG_USER_COMMENT,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED
            )
            tags.forEach { tag ->
                try {
                    exif.setAttribute(tag, null)
                } catch (_: Exception) {
                }
            }
        } else {
            if (options.stripLocation) {
                listOf(
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_ALTITUDE,
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    ExifInterface.TAG_GPS_PROCESSING_METHOD,
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    ExifInterface.TAG_GPS_DATESTAMP
                ).forEach {
                    try {
                        exif.setAttribute(it, null)
                    } catch (_: Exception) {
                    }
                }
            }
            if (options.stripCameraInfo) {
                listOf(
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_SOFTWARE,
                    ExifInterface.TAG_ARTIST
                ).forEach {
                    try {
                        exif.setAttribute(it, null)
                    } catch (_: Exception) {
                    }
                }
            }
        }
        exif.saveAttributes()
    }

    fun buildCaption(
        item: MediaItem,
        exif: ExifInfo,
        options: MetadataOptions
    ): String {
        val lines = mutableListOf<String>()
        if (options.includeFileNameInCaption) {
            val icon = if (item.isVideo) "🎬" else "📷"
            lines += "$icon ${item.name}"
        }
        if (options.includeFolderInCaption && item.folderName.isNotBlank()) {
            lines += "📁 ${item.folderName}"
        }
        if (options.includeSizeInCaption) {
            lines += "💾 ${item.sizeLabel}"
        }
        if (item.isVideo && item.durationLabel.isNotBlank()) {
            lines += "⏱ ${item.durationLabel}"
        }
        if (options.includeDateInCaption) {
            val dateStr = exif.dateTime
                ?: if (item.dateAdded > 0) {
                    SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                        .format(Date(item.dateAdded * 1000))
                } else null
            if (!dateStr.isNullOrBlank()) lines += "📅 $dateStr"
        }
        if (options.includeCameraInCaption) {
            val cam = listOfNotNull(exif.make, exif.model).joinToString(" ").trim()
            if (cam.isNotBlank()) lines += "📸 $cam"
        }
        if (options.includeLocationInCaption) {
            val lat = exif.latitude ?: item.latitude
            val lon = exif.longitude ?: item.longitude
            if (lat != null && lon != null) {
                lines += "📍 %.5f, %.5f".format(lat, lon)
                lines += "🗺 https://maps.google.com/?q=$lat,$lon"
            }
        }
        // privacy note when stripped
        if (options.stripAllExif || options.stripLocation || !options.keepOriginalFile) {
            val stripped = buildList {
                if (options.stripAllExif || !options.keepOriginalFile) add("EXIF removido")
                else {
                    if (options.stripLocation) add("GPS removido")
                    if (options.stripCameraInfo) add("câmera removida")
                }
            }
            if (stripped.isNotEmpty()) lines += "🔒 ${stripped.joinToString(" · ")}"
        }
        return lines.joinToString("\n").take(1024)
    }
}
