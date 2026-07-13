package com.telebackup.app.util

import android.content.Context
import android.net.Uri
import android.os.Build
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import coil.size.Scale
import coil.size.Size

object ImageLoading {

    @Volatile
    private var loader: ImageLoader? = null

    fun imageLoader(context: Context): ImageLoader {
        return loader ?: synchronized(this) {
            loader ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .crossfade(120)
                .respectCacheHeaders(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .allowHardware(Build.VERSION.SDK_INT >= 28)
                .build()
                .also { loader = it }
        }
    }

    /** Lightweight thumbnail for grid cells (~120–160dp @xxhdpi ≈ 360–480px). */
    fun thumbRequest(
        context: Context,
        uri: Uri,
        isVideo: Boolean = false,
        sizePx: Int = 420
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(uri)
            .size(Size(sizePx, sizePx))
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .memoryCacheKey("thumb_${uri}_$sizePx")
            .diskCacheKey("thumb_${uri}_$sizePx")
            .crossfade(100)
            .allowHardware(true)
            .apply {
                if (isVideo) {
                    videoFrameMillis(0)
                }
            }
            .build()
    }

    /** Fullscreen viewer request — larger decode, still capped for RAM. */
    fun fullRequest(
        context: Context,
        uri: Uri,
        maxSide: Int = 2048
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(uri)
            .size(Size(maxSide, maxSide))
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .memoryCacheKey("full_$uri")
            .crossfade(false)
            .allowHardware(true)
            .build()
    }
}
