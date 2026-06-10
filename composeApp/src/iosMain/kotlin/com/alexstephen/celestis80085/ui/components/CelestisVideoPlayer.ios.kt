package com.alexstephen.celestis80085.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.unit.dp
import com.alexstephen.celestis80085.ui.utils.VideoUrlUtils
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import kotlin.coroutines.resume

/**
 * iOS implementation of CelestisVideoPlayer.
 *
 * - YouTube videos: Embedded via WKWebView iframe player
 * - Direct media (MP4/HLS): AVURLAsset with async metadata pre-loading off the main thread,
 *   then AVPlayerItem + AVPlayer created back on the main thread to avoid the
 *   "Main thread blocked by synchronous property query on not-yet-loaded property
 *   (PreferredTransform)" warning.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CelestisVideoPlayer(
    videoUrl: String,
    modifier: Modifier,
    onError: (String) -> Unit,
    isPlaying: Boolean,
    isLandscape: Boolean,
    onPlayingChange: (Boolean) -> Unit,
) {
    val isYouTube = remember(videoUrl) { VideoUrlUtils.isYouTubeUrl(videoUrl) }
    val youtubeVideoId = remember(videoUrl) {
        if (isYouTube) VideoUrlUtils.extractYouTubeId(videoUrl) else null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isYouTube) {
            if (youtubeVideoId != null) {
                val embedHtml = remember(youtubeVideoId) {
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        * { margin: 0; padding: 0; }
                        html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                        iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
                    </style>
                    </head>
                    <body>
                    <iframe
                        src="https://www.youtube.com/embed/$youtubeVideoId?playsinline=1&rel=0&modestbranding=1"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                        allowfullscreen>
                    </iframe>
                    </body>
                    </html>
                    """.trimIndent()
                }

                UIKitView(
                    factory = {
                        val config = WKWebViewConfiguration().apply {
                            allowsInlineMediaPlayback = true
                            mediaTypesRequiringUserActionForPlayback = 0u
                        }
                        WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                            scrollView.scrollEnabled = false
                            scrollView.bounces = false
                            loadHTMLString(embedHtml, baseURL = null)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { _ -> }
                )
            } else {
                LaunchedEffect(Unit) { onError("Could not extract YouTube video ID from URL") }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Video unavailable", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        } else {
            // Direct media (MP4 / HLS)
            var player by remember(videoUrl) { mutableStateOf<AVPlayer?>(null) }
            var loadError by remember(videoUrl) { mutableStateOf<String?>(null) }

            LaunchedEffect(videoUrl) {
                val nsUrl = NSURL.URLWithString(videoUrl)
                if (nsUrl == null) {
                    loadError = "Invalid video URL"
                    return@LaunchedEffect
                }

                try {
                    val asset = AVURLAsset.URLAssetWithURL(nsUrl, options = null)

                    // Load all required keys — including preferredTransform — asynchronously
                    // on a background thread. This prevents the main thread from blocking on
                    // a synchronous property query for not-yet-loaded AVAsset properties.
                    // "preferredTransform" is explicitly pre-loaded here so that when
                    // AVPlayerViewController later reads it, it is already cached and the
                    // access is non-blocking.
                    val keysToLoad = listOf("playable", "tracks", "duration", "preferredTransform")

                    withContext(Dispatchers.Default) {
                        suspendCancellableCoroutine<Unit> { cont ->
                            asset.loadValuesAsynchronouslyForKeys(keysToLoad) {
                                cont.resume(Unit)
                            }
                        }
                    }

                    // AVPlayerItem and AVPlayer must be created on the main thread after
                    // all keys have been loaded asynchronously above.
                    withContext(Dispatchers.Main) {
                        val item = AVPlayerItem(asset = asset)
                        player = AVPlayer(playerItem = item)
                    }
                } catch (e: Exception) {
                    loadError = e.message ?: "Failed to load video"
                }
            }

            val currentPlayer = player

            if (loadError != null) {
                LaunchedEffect(loadError) { onError(loadError!!) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Unable to load video", color = MaterialTheme.colorScheme.onBackground)
                }
            } else if (currentPlayer != null) {
                LaunchedEffect(isPlaying) {
                    if (isPlaying) currentPlayer.play() else currentPlayer.pause()
                }

                DisposableEffect(currentPlayer) {
                    onDispose { currentPlayer.pause() }
                }

                // UIKitViewController manages the full UIViewController lifecycle of
                // AVPlayerViewController, preventing premature deallocation.
                UIKitViewController(
                    factory = {
                        AVPlayerViewController().apply {
                            this.player = currentPlayer
                            showsPlaybackControls = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { controller ->
                        controller.player = currentPlayer
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
