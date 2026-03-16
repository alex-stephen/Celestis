package com.example.astrolume.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.astrolume.ui.utils.CommonBackHandler

@Composable
fun HdImagePopup(
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    CommonBackHandler(enabled = true, onBack = onDismiss)

    // Transformation States
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Smooth return animations for double-tap or snap-back
    val animatedScale by animateFloatAsState(targetValue = scale, label = "Scale")
    val animatedOffset by animateOffsetAsState(targetValue = offset, label = "Offset")

    // The Background Layer: Acts as a touch shield for the content underneath
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                // Consumes taps on the darkened background to dismiss
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "HD Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffset.x,
                    translationY = animatedOffset.y
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Anchor the gesture by awaiting the first touch
                        awaitFirstDown(requireUnconsumed = false)

                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) continue

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            // 1. Update Scale
                            scale = (scale * zoomChange).coerceIn(1f, 5f)

                            // 2. Calculate Boundaries
                            // We only pan if zoomed in. Max pan is half the 'overflow' size.
                            val extraWidth = (size.width * scale) - size.width
                            val extraHeight = (size.height * scale) - size.height
                            val maxX = (extraWidth / 2).coerceAtLeast(0f)
                            val maxY = (extraHeight / 2).coerceAtLeast(0f)

                            if (scale > 1f) {
                                // Apply Panning with hard boundaries
                                val newOffset = offset + panChange
                                offset = Offset(
                                    x = newOffset.x.coerceIn(-maxX, maxX),
                                    y = newOffset.y.coerceIn(-maxY, maxY)
                                )
                            } else {
                                // Rubber Banding for Swipe-to-Dismiss
                                val resistance = 0.4f
                                offset = Offset(0f, offset.y + (panChange.y * resistance))
                            }

                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })

                        // Reset or Dismiss logic
                        if (scale == 1f) {
                            if (offset.y > 150f || offset.y < -150f) {
                                onDismiss()
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // Keep double-tap separate as it's a discrete event
                    detectTapGestures(
                        onDoubleTap = { centroid ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 3f
                            }
                        },
                        onTap = { /* Prevent background dismissal */ }
                    )
                },
            loading = {
                HdImageLoadingIndicator()
            }
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}