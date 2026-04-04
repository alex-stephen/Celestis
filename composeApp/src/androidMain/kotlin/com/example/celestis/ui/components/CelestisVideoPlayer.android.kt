package com.example.celestis.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.celestis.ui.utils.VideoUrlUtils
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * Modern Android implementation of CelestisVideoPlayer.
 * * Architecture:
 * - YouTube: Uses android-youtube-player for a sandboxed, native-feeling bridge.
 * Prevents users from navigating away to the YouTube website.
 * - Direct MP4s: Uses Media3 ExoPlayer with strict lifecycle bindings.
 * * Safety & Performance:
 * - Bound to Compose LocalLifecycleOwner to pause on backgrounding.
 * - Proper instance disposal to prevent memory and audio leaks.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
actual fun CelestisVideoPlayer(
    videoUrl: String,
    modifier: Modifier,
    onError: (String) -> Unit,
    isPlaying: Boolean,
    isLandscape: Boolean,
    onPlayingChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val isYouTube = remember(videoUrl) { VideoUrlUtils.isYouTubeUrl(videoUrl) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isYouTube) {
            val youtubeVideoId = remember(videoUrl) { VideoUrlUtils.extractYouTubeId(videoUrl) }

            // store a reference so we can call play/pause on it
            var youTubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }

            // wire isPlaying to the stored reference
            LaunchedEffect(isPlaying, youTubePlayerRef) {
                val player = youTubePlayerRef ?: return@LaunchedEffect
                if (isPlaying) player.play() else player.pause()
            }

            if (youtubeVideoId != null) {
                AndroidView(
                    factory = { ctx ->
                        YouTubePlayerView(ctx).apply {
                            lifecycleOwner.lifecycle.addObserver(this)

                            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    youTubePlayerRef = youTubePlayer  // ← store ref
                                    youTubePlayer.cueVideo(youtubeVideoId, 0f)
                                }

                                override fun onError(
                                    youTubePlayer: YouTubePlayer,
                                    error: PlayerConstants.PlayerError
                                ) {
                                    onError("YouTube Player Error: ${error.name}")
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(top = if (isLandscape) 175.dp else 0.dp),
                    onRelease = { view ->
                        youTubePlayerRef = null  // ← clear ref on release
                        lifecycleOwner.lifecycle.removeObserver(view)
                        view.release()
                    }
                )
            } else {
                onError("Could not extract YouTube video ID from URL")
            }
        } else {
            val exoPlayer = remember(videoUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(videoUrl))
                    prepare()
                    playWhenReady = false

                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            onError("Video playback failed: ${error.message}")
                        }
                    })
                }
            }

            LaunchedEffect(isPlaying) {
                if (isPlaying) exoPlayer.play() else exoPlayer.pause()
            }

            DisposableEffect(exoPlayer, lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                        Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    exoPlayer.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false  // ← FIXED: Spacer owns all taps now
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}