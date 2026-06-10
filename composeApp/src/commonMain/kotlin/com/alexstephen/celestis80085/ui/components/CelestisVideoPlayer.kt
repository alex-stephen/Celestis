package com.alexstephen.celestis80085.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * CelestisVideoPlayer - Cross-platform video player for APOD videos
 * 
 * Expect/Actual pattern allows platform-specific implementations:
 * - Android: Media3 ExoPlayer
 * - iOS: AVPlayerViewController
 * 
 * Supports full-screen mode in both portrait and landscape orientations.
 */
@Composable
expect fun CelestisVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {},
    isPlaying: Boolean = false,
    isLandscape: Boolean = false,
    onPlayingChange: (Boolean) -> Unit = {},
)
