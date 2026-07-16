package com.telebackup.app.data

/**
 * Groups media for cleanup: exact duplicates and smart categories.
 */
object CleanupAnalyzer {

    data class DuplicateGroup(
        val key: String,
        val items: List<MediaItem>,
        val keep: MediaItem,
        val extras: List<MediaItem>
    ) {
        val wasteBytes: Long get() = extras.sumOf { it.size }
        val count: Int get() = items.size
    }

    data class CategoryBucket(
        val id: String,
        val title: String,
        val subtitle: String,
        val items: List<MediaItem>,
        val emoji: String = "📁"
    ) {
        val totalBytes: Long get() = items.sumOf { it.size }
        val count: Int get() = items.size
        val sizeLabel: String get() = MediaItem.formatSize(totalBytes)
    }

    fun findDuplicates(media: List<MediaItem>): List<DuplicateGroup> {
        // Exact-ish duplicates: same name (case-insensitive) + same size
        // Also group by size-only for same-size clones with different names if name base matches
        val byNameSize = media
            .filter { it.size > 0 }
            .groupBy { "${it.name.lowercase().trim()}|${it.size}" }
            .filter { it.value.size > 1 }

        return byNameSize.map { (key, list) ->
            val sorted = list.sortedWith(
                compareByDescending<MediaItem> { it.dateAdded }
                    .thenByDescending { it.id }
            )
            // Keep newest
            val keep = sorted.first()
            val extras = sorted.drop(1)
            DuplicateGroup(key = key, items = sorted, keep = keep, extras = extras)
        }.sortedByDescending { it.wasteBytes }
    }

    fun categories(media: List<MediaItem>): List<CategoryBucket> {
        fun match(item: MediaItem, vararg needles: String): Boolean {
            val hay = (item.folderName + " " + item.relativePath + " " + item.name).lowercase()
            return needles.any { hay.contains(it.lowercase()) }
        }

        val screenshots = media.filter {
            it.isImage && match(it, "screenshot", "screenshots", "captura", "screen_shot")
        }
        val whatsapp = media.filter {
            match(it, "whatsapp", "waimages", "wa video", "wa/")
        }
        val telegram = media.filter {
            match(it, "telegram", "telegram images", "telegram video")
        }
        val downloads = media.filter {
            match(it, "download", "downloads")
        }
        val camera = media.filter {
            match(it, "dcim", "camera", "100andro", "img_")
        }
        val large = media.filter { it.size >= 8L * 1024 * 1024 } // >= 8MB
        val videos = media.filter { it.isVideo }
        val old = media.filter {
            // older than ~1 year (approx using dateAdded seconds)
            val yearAgo = (System.currentTimeMillis() / 1000L) - 365L * 24 * 3600
            it.dateAdded in 1 until yearAgo
        }
        val blurryCandidates = media.filter {
            // small resolution-ish proxy: very small image files under 80KB often junk
            it.isImage && it.size in 1..(80 * 1024)
        }

        // Group by folder bucket
        val byFolder = media
            .groupBy { it.folderName.ifBlank { "Outros" } }
            .map { (name, items) ->
                CategoryBucket(
                    id = "folder:$name",
                    title = name,
                    subtitle = "Pasta / álbum",
                    items = items.sortedByDescending { it.dateAdded },
                    emoji = "📂"
                )
            }
            .sortedByDescending { it.count }
            .take(30)

        val smart = listOf(
            CategoryBucket("screenshots", "Capturas de tela", "Screenshots e prints", screenshots, "📱"),
            CategoryBucket("whatsapp", "WhatsApp", "Imagens e vídeos do WhatsApp", whatsapp, "💬"),
            CategoryBucket("telegram", "Telegram", "Mídias salvas do Telegram", telegram, "✈️"),
            CategoryBucket("downloads", "Downloads", "Arquivos baixados", downloads, "⬇️"),
            CategoryBucket("camera", "Câmera / DCIM", "Fotos da câmera", camera, "📷"),
            CategoryBucket("videos", "Todos os vídeos", "Arquivos de vídeo", videos, "🎬"),
            CategoryBucket("large", "Arquivos grandes (≥8 MB)", "Ocupam mais espaço", large, "📦"),
            CategoryBucket("old", "Mais antigos (1+ ano)", "Mídias antigas", old, "🗓️"),
            CategoryBucket("tiny", "Imagens minúsculas", "Possíveis lixos / thumbs", blurryCandidates, "🧹")
        ).filter { it.items.isNotEmpty() }

        return smart + byFolder.filter { bucket ->
            // avoid duplicating smart categories already covered
            smart.none { it.title.equals(bucket.title, true) }
        }
    }

    fun totalWaste(duplicates: List<DuplicateGroup>): Long =
        duplicates.sumOf { it.wasteBytes }
}
