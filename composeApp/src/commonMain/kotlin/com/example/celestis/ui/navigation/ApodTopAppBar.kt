package com.example.celestis.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * @param isVideoSource When true the underlying hazeSource is an opaque native video layer
 *   that cannot be sampled by the haze effect. In that case we fall back to a solid dark
 *   background so the bar remains legible instead of rendering as fully transparent.
 */
@Composable
fun ApodTopAppBar(
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    titleContent: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    windowSizeClass: WindowSizeClass? = null,
    hazeState: HazeState,
    isVideoSource: Boolean = false
) {
    val isLandscape = windowSizeClass?.widthSizeClass != WindowWidthSizeClass.Compact
    val appBarContentHeight = if (isLandscape) 42.dp else 65.dp

    // When the content behind the bar is a native video layer (UIKitViewController /
    // ExoPlayer SurfaceView) the Compose haze effect cannot sample pixels from it.
    // We therefore apply a plain dark background as a fallback so the bar stays legible.
    val barModifier = if (isVideoSource) {
        modifier
            .fillMaxWidth()
            .background(Color(0xFF111111).copy(alpha = 0.92f))
            .drawBehind {
                val strokeWidthPx = 1.dp.toPx()
                val verticalOffset = size.height - strokeWidthPx / 2
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, verticalOffset),
                    end = Offset(size.width, verticalOffset),
                    strokeWidth = strokeWidthPx
                )
            }
    } else {
        modifier
            .fillMaxWidth()
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFF111111).copy(alpha = 0.85f),
                    blurRadius = 30.dp,
                    noiseFactor = 0f,
                    tint = HazeTint.Unspecified,
                )
            )
            .drawBehind {
                val strokeWidthPx = 1.dp.toPx()
                val verticalOffset = size.height - strokeWidthPx / 2
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, verticalOffset),
                    end = Offset(size.width, verticalOffset),
                    strokeWidth = strokeWidthPx
                )
            }
    }

    // Outer box: Background extends through status bar
    Box(modifier = barModifier) {
        // Inner box: Content padded for status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(appBarContentHeight)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Left Slot (Menu/Back)
            if (navigationIcon != null) {
                Box(Modifier.align(Alignment.CenterStart)) {
                    navigationIcon()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                titleContent()
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
