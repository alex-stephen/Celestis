package com.example.celestis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

/**
 * iOS implementation of CelestisVideoPlayer using AVPlayerViewController.
 * 
 * Features:
 * - Native iOS video playback
 * - Full-screen support (portrait & landscape)
 * - Proper lifecycle management
 * - YouTube URL support (may need web view for some cases)
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
    val player = remember {
        val url = NSURL.URLWithString(videoUrl)
        if (url != null) {
            val playerItem = AVPlayerItem.playerItemWithURL(url)
            AVPlayer.playerWithPlayerItem(playerItem)
        } else {
            onError("Invalid video URL")
            null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) player?.play() else player?.pause()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            player?.pause()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (player != null) {
            UIKitView(
                factory = {
                    val playerViewController = AVPlayerViewController()
                    playerViewController.player = player
                    playerViewController.showsPlaybackControls = true
                    playerViewController.view
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Update logic if needed
                }
            )
        }
    }
}
