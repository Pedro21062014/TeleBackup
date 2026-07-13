package com.telebackup.app.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.telebackup.app.data.MediaItem
import com.telebackup.app.ui.theme.NightElevated
import com.telebackup.app.ui.theme.TelegramBlue
import com.telebackup.app.ui.theme.TextMuted
import com.telebackup.app.ui.theme.TextSecondary
import com.telebackup.app.util.ImageLoading
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    items: List<MediaItem>,
    initial: MediaItem,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val safeItems = remember(items) { items.ifEmpty { listOf(initial) } }
    val startIndex = remember(initial.id, safeItems) {
        safeItems.indexOfFirst { it.id == initial.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { safeItems.size }
    )
    val scope = rememberCoroutineScope()
    var showChrome by remember { mutableStateOf(true) }
    // When any page is zoomed, disable pager swipe so pan works
    var zoomedPage by remember { mutableStateOf(false) }

    val current = safeItems.getOrNull(pagerState.currentPage) ?: initial

    // Reset zoom lock when page changes
    LaunchedEffect(pagerState.currentPage) {
        zoomedPage = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> safeItems.getOrNull(page)?.id ?: page },
            beyondBoundsPageCount = 1,
            userScrollEnabled = !zoomedPage,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
        ) { page ->
            val item = safeItems[page]
            val isActive = pagerState.currentPage == page
            if (item.isVideo) {
                VideoPlayer(
                    uri = item.uri,
                    isActive = isActive,
                    onTap = { showChrome = !showChrome }
                )
            } else {
                ZoomableImage(
                    item = item,
                    isActive = isActive,
                    onTap = { showChrome = !showChrome },
                    onZoomChanged = { zoomed ->
                        if (isActive) zoomedPage = zoomed
                    }
                )
            }
        }

        // Side peek hints / nav buttons
        AnimatedVisibility(
            visible = showChrome && safeItems.size > 1 && !zoomedPage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.ChevronLeft,
                        contentDescription = "Anterior",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .padding(4.dp)
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showChrome && safeItems.size > 1 && !zoomedPage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            if (pagerState.currentPage < safeItems.lastIndex) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "Próxima",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .padding(4.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Fechar",
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .padding(6.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        current.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        buildString {
                            append(pagerState.currentPage + 1)
                            append(" / ")
                            append(safeItems.size)
                            append(if (current.isVideo) " · vídeo" else " · foto")
                            append(" · arraste ← →")
                        },
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .padding(20.dp)
            ) {
                InfoPill(
                    if (current.isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                    current.name
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(Icons.Outlined.SdStorage, current.sizeLabel)
                    if (current.folderName.isNotBlank()) {
                        InfoChip(Icons.Outlined.Folder, current.folderName)
                    }
                    if (current.isVideo && current.durationLabel.isNotBlank()) {
                        InfoChip(Icons.Outlined.Videocam, current.durationLabel)
                    }
                    if (current.latitude != null && current.longitude != null) {
                        InfoChip(
                            Icons.Outlined.LocationOn,
                            "%.3f, %.3f".format(current.latitude, current.longitude)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    item: MediaItem,
    isActive: Boolean,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val loader = remember(context) { ImageLoading.imageLoader(context) }
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }

    // Reset when leaving page — critical so swipe works again
    LaunchedEffect(isActive, item.id) {
        if (!isActive) {
            scale = 1f
            offset = Offset.Zero
            onZoomChanged(false)
        }
    }

    val request = remember(item.uri) { ImageLoading.fullRequest(context, item.uri) }

    // When not zoomed: only taps/double-tap — single-finger drag goes to HorizontalPager.
    // When zoomed: pan + pinch consume gestures and pager is disabled via onZoomChanged.
    val baseModifier = Modifier.fillMaxSize()
    val gestureModifier = if (scale > 1.01f) {
        baseModifier
            .pointerInput(item.id, scale) {
                detectTapGestures(
                    onTap = {
                        scale = 1f
                        offset = Offset.Zero
                        onZoomChanged(false)
                    },
                    onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                        onZoomChanged(false)
                    }
                )
            }
            .pointerInput(item.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1.01f) {
                        offset += pan
                        onZoomChanged(true)
                    } else {
                        scale = 1f
                        offset = Offset.Zero
                        onZoomChanged(false)
                    }
                }
            }
    } else {
        baseModifier.pointerInput(item.id) {
            detectTapGestures(
                onTap = { onTap() },
                onDoubleTap = {
                    scale = 2.5f
                    onZoomChanged(true)
                }
            )
        }
    }

    Box(
        modifier = gestureModifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = request,
            contentDescription = item.name,
            imageLoader = loader,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
private fun VideoPlayer(uri: Uri, isActive: Boolean, onTap: () -> Unit) {
    var playing by remember(uri) { mutableStateOf(false) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(uri) {
        onDispose {
            try {
                videoView?.stopPlayback()
            } catch (_: Exception) {
            }
            videoView = null
        }
    }

    // Pause when not active page
    LaunchedEffect(isActive) {
        val vv = videoView
        if (vv != null) {
            if (isActive) {
                // don't auto-start aggressively; user taps
            } else {
                if (vv.isPlaying) {
                    vv.pause()
                    playing = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = {
                        val vv = videoView
                        if (vv != null) {
                            if (vv.isPlaying) {
                                vv.pause()
                                playing = false
                            } else {
                                vv.start()
                                playing = true
                            }
                        }
                        onTap()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            // show first frame, don't autoplay until tap if preferred
                            seekTo(1)
                            playing = false
                        }
                        setOnErrorListener { _, _, _ -> true }
                        videoView = this
                    }
                },
                update = { view ->
                    videoView = view
                    if (!isActive && view.isPlaying) {
                        view.pause()
                        playing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Lightweight placeholder for offscreen pages (big perf win)
            val context = LocalContext.current
            val loader = remember(context) { ImageLoading.imageLoader(context) }
            AsyncImage(
                model = ImageLoading.thumbRequest(context, uri, isVideo = true, sizePx = 720),
                contentDescription = null,
                imageLoader = loader,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
        }

        if (isActive && !playing) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NightElevated.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = TelegramBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = TextSecondary, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
