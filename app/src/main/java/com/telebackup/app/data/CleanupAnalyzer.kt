package com.telebackup.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.abs
import kotlin.math.min

/**
 * Groups media for cleanup: exact duplicates, similar photos (aHash), and categories.
 * Picks the best copy to keep and marks the rest as removable.
 */
object CleanupAnalyzer {

    data class DuplicateGroup(
        val key: String,
        val items: List<MediaItem>,
        val keep: MediaItem,
        val extras: List<MediaItem>,
        val kind: Kind = Kind.Exact
    ) {
        val wasteBytes: Long get() = extras.sumOf { it.size }
        val count: Int get() = items.size

        enum class Kind { Exact, Similar }
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

    /** Exact duplicates: same filename (case-insensitive) + same size. */
    fun findExactDuplicates(media: List<MediaItem>): List<DuplicateGroup> {
        val byNameSize = media
            .filter { it.size > 0 }
            .groupBy { "${it.name.lowercase().trim()}|${it.size}" }
            .filter { it.value.size > 1 }

        return byNameSize.map { (key, list) ->
            val ranked = list.sortedByDescending { qualityScore(it) }
            val keep = ranked.first()
            val extras = ranked.drop(1)
            DuplicateGroup(
                key = "exact:$key",
                items = ranked,
                keep = keep,
                extras = extras,
                kind = DuplicateGroup.Kind.Exact
            )
        }.sortedByDescending { it.wasteBytes }
    }

    /**
     * Similar photos via average-hash (8x8).
     * Hamming distance <= [maxDistance] counts as similar.
     * Keeps the highest qualityScore item; rest are extras.
     */
    fun findSimilarPhotos(
        context: Context,
        media: List<MediaItem>,
        maxDistance: Int = 8,
        maxToHash: Int = 4000
    ): List<DuplicateGroup> {
        val images = media
            .asSequence()
            .filter { it.isImage && it.size > 2_000L }
            // skip tiny thumbs already handled as category
            .sortedByDescending { it.dateAdded }
            .take(maxToHash)
            .toList()

        if (images.size < 2) return emptyList()

        val hashes = ArrayList<Pair<MediaItem, Long>>(images.size)
        for (item in images) {
            val h = averageHash(context, item) ?: continue
            hashes += item to h
        }
        if (hashes.size < 2) return emptyList()

        // Union-find for clustering similar hashes
        val n = hashes.size
        val parent = IntArray(n) { it }
        fun find(x: Int): Int {
            var i = x
            while (parent[i] != i) {
                parent[i] = parent[parent[i]]
                i = parent[i]
            }
            return i
        }
        fun union(a: Int, b: Int) {
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) parent[rb] = ra
        }

        // O(n^2) but n capped; for large sets compare only within size bands
        val bySizeBand = hashes.withIndex().groupBy { (_, pair) ->
            // band by log-ish size to cut comparisons
            val s = pair.first.size
            when {
                s < 100_000 -> 0
                s < 400_000 -> 1
                s < 1_500_000 -> 2
                s < 5_000_000 -> 3
                else -> 4
            }
        }

        for (band in bySizeBand.values) {
            val list = band // List of IndexedValue
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    val ia = list[i].index
                    val ib = list[j].index
                    val ha = list[i].value.second
                    val hb = list[j].value.second
                    if (hamming(ha, hb) <= maxDistance) {
                        union(ia, ib)
                    }
                }
            }
        }

        // Also cross-band neighbors (size within 40%)
        // lightweight: only if same name stem
        val byStem = hashes.withIndex().groupBy { (_, p) -> nameStem(p.first.name) }
        for ((stem, list) in byStem) {
            if (stem.length < 3 || list.size < 2) continue
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    val a = list[i].value.first
                    val b = list[j].value.first
                    val sa = a.size.toDouble()
                    val sb = b.size.toDouble()
                    val ratio = min(sa, sb) / maxOf(sa, sb)
                    if (ratio < 0.5) continue
                    if (hamming(list[i].value.second, list[j].value.second) <= maxDistance + 2) {
                        union(list[i].index, list[j].index)
                    }
                }
            }
        }

        val clusters = hashes.indices.groupBy { find(it) }.values
            .map { idxs -> idxs.map { hashes[it].first } }
            .filter { it.size > 1 }

        // Drop clusters that are already exact name+size (will appear in exact list)
        return clusters.map { list ->
            val ranked = list.sortedByDescending { qualityScore(it) }
            val keep = ranked.first()
            val extras = ranked.drop(1)
            DuplicateGroup(
                key = "sim:${keep.id}:${extras.joinToString("-") { it.id.toString() }.take(40)}",
                items = ranked,
                keep = keep,
                extras = extras,
                kind = DuplicateGroup.Kind.Similar
            )
        }.sortedByDescending { it.wasteBytes }
    }

    fun mergeDuplicateLists(
        exact: List<DuplicateGroup>,
        similar: List<DuplicateGroup>
    ): List<DuplicateGroup> {
        // Prefer exact groups; exclude similar items already covered as exact extras/keeps
        val covered = exact.flatMap { it.items.map { m -> m.id } }.toHashSet()
        val filteredSimilar = similar.mapNotNull { g ->
            val remaining = g.items.filter { it.id !in covered }
            if (remaining.size < 2) return@mapNotNull null
            val ranked = remaining.sortedByDescending { qualityScore(it) }
            g.copy(items = ranked, keep = ranked.first(), extras = ranked.drop(1))
        }
        return (exact + filteredSimilar).sortedByDescending { it.wasteBytes }
    }

    /**
     * Higher is better: prefer larger files, newer, camera folders, not screenshots/downloads.
     */
    fun qualityScore(item: MediaItem): Double {
        var score = 0.0
        // Size dominates (higher resolution usually bigger)
        score += item.size / 1024.0
        // Recency (seconds since epoch — small weight)
        score += item.dateAdded / 1000.0
        val path = (item.folderName + " " + item.relativePath + " " + item.name).lowercase()
        when {
            path.contains("screenshot") || path.contains("captura") -> score -= 50_000
            path.contains("download") -> score -= 10_000
            path.contains("whatsapp") -> score -= 5_000
            path.contains("dcim") || path.contains("camera") -> score += 25_000
            path.contains("img_") || path.contains("dsc") -> score += 10_000
        }
        // Prefer original-looking extensions
        when {
            item.name.endsWith(".jpg", true) || item.name.endsWith(".jpeg", true) -> score += 2_000
            item.name.endsWith(".heic", true) || item.name.endsWith(".png", true) -> score += 3_000
            item.name.endsWith(".webp", true) -> score -= 1_000
        }
        // Very small images are worse
        if (item.size < 80_000) score -= 20_000
        return score
    }

    fun categories(media: List<MediaItem>): List<CategoryBucket> {
        fun match(item: MediaItem, vararg needles: String): Boolean {
            val hay = (item.folderName + " " + item.relativePath + " " + item.name).lowercase()
            return needles.any { hay.contains(it.lowercase()) }
        }

        val screenshots = media.filter {
            it.isImage && match(it, "screenshot", "screenshots", "captura", "screen_shot")
        }
        val whatsapp = media.filter { match(it, "whatsapp", "waimages", "wa video", "wa/") }
        val telegram = media.filter { match(it, "telegram", "telegram images", "telegram video") }
        val downloads = media.filter { match(it, "download", "downloads") }
        val camera = media.filter { match(it, "dcim", "camera", "100andro", "img_") }
        val large = media.filter { it.size >= 8L * 1024 * 1024 }
        val videos = media.filter { it.isVideo }
        val old = media.filter {
            val yearAgo = (System.currentTimeMillis() / 1000L) - 365L * 24 * 3600
            it.dateAdded in 1 until yearAgo
        }
        val blurryCandidates = media.filter {
            it.isImage && it.size in 1..(80 * 1024)
        }

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
            smart.none { it.title.equals(bucket.title, true) }
        }
    }

    fun totalWaste(duplicates: List<DuplicateGroup>): Long =
        duplicates.sumOf { it.wasteBytes }

    // ── Image hashing ───────────────────────────────────────────────

    private fun averageHash(context: Context, item: MediaItem): Long? {
        return try {
            val opts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(item.uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val w = opts.outWidth
            val h = opts.outHeight
            if (w <= 0 || h <= 0) return null
            val sample = maxOf(1, min(w, h) / 64)

            val decode = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val original = context.contentResolver.openInputStream(item.uri)?.use {
                BitmapFactory.decodeStream(it, null, decode)
            } ?: return null

            val small = Bitmap.createScaledBitmap(original, 8, 8, true)
            if (original !== small) original.recycle()

            // grayscale average
            val pixels = IntArray(64)
            small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
            small.recycle()

            var sum = 0L
            val gray = IntArray(64)
            for (i in 0 until 64) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val y = (r * 30 + g * 59 + b * 11) / 100
                gray[i] = y
                sum += y
            }
            val avg = (sum / 64).toInt()
            var hash = 0L
            for (i in 0 until 64) {
                if (gray[i] >= avg) hash = hash or (1L shl i)
            }
            hash
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private fun hamming(a: Long, b: Long): Int =
        java.lang.Long.bitCount(a xor b)

    private fun nameStem(name: String): String {
        val base = name.substringBeforeLast('.', name).lowercase()
        // strip trailing _1, (1), copy, etc.
        return base
            .replace(Regex("""[\s_-]*(copy|copia|\(\d+\)|_\d+)$"""), "")
            .replace(Regex("""\d{8,}$"""), "")
            .trim()
    }
}
