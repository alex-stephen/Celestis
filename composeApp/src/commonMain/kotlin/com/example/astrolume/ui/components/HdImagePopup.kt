package com.example.astrolume.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.example.astrolume.ui.utils.CommonBackHandler
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun HdImagePopup(
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    CommonBackHandler(enabled = true, onBack = onDismiss)

    val scope = rememberCoroutineScope()
    var isImageLoading by remember { mutableStateOf(true) }

    // Transformation state using Animatable for smooth physics
    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    
    // Container size for boundary calculations
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Min and max scale limits
    val minScale = 1f
    val maxScale = 5f

    // Calculate boundaries to prevent over-panning
    fun calculateMaxOffset(): Offset {
        val maxX = ((containerSize.width * scale.value) - containerSize.width).coerceAtLeast(0f) / 2f
        val maxY = ((containerSize.height * scale.value) - containerSize.height).coerceAtLeast(0f) / 2f
        return Offset(maxX, maxY)
    }

    // Apply boundaries to current offset
    fun constrainOffset(currentOffset: Offset): Offset {
        val maxOffset = calculateMaxOffset()
        return Offset(
            x = currentOffset.x.coerceIn(-maxOffset.x, maxOffset.x),
            y = currentOffset.y.coerceIn(-maxOffset.y, maxOffset.y)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Only dismiss if not zoomed
                        if (scale.value <= 1.01f) {
                            onDismiss()
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "HD Image",
            contentScale = ContentScale.Fit,
            onState = { state ->
                isImageLoading = state is AsyncImagePainter.State.Loading
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offset.value.x
                    translationY = offset.value.y
                }
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        velocityTracker.resetTracking()
                        
                        var zoom = scale.value
                        var pan = offset.value
                        var isZooming = false
                        var lastPointerCount = 1
                        
                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            
                            if (!canceled) {
                                val currentPointerCount = event.changes.size
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                
                                // Track if we're actively zooming
                                if (zoomChange != 1f) {
                                    isZooming = true
                                }
                                
                                // Handle zoom
                                if (zoomChange != 1f) {
                                    val newZoom = (zoom * zoomChange).coerceIn(minScale, maxScale)
                                    
                                    // Calculate focal point zoom - zoom centered on fingers
                                    val focusPoint = centroid - Offset(
                                        containerSize.width / 2f,
                                        containerSize.height / 2f
                                    )
                                    
                                    // Adjust pan to keep focal point stable
                                    pan = (pan + focusPoint) * (newZoom / zoom) - focusPoint
                                    zoom = newZoom
                                }
                                
                                // Handle pan
                                if (panChange != Offset.Zero) {
                                    pan += panChange
                                }
                                
                                // Constrain offset
                                pan = constrainOffset(pan)
                                
                                // Update display immediately - no coroutine delay for responsiveness
                                scale.updateBounds(zoom, zoom)
                                offset.updateBounds(pan, pan)
                                
                                // Only track velocity when panning (not zooming) with single finger
                                if (!isZooming && currentPointerCount == 1 && lastPointerCount == 1) {
                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            velocityTracker.addPosition(
                                                change.uptimeMillis,
                                                change.position
                                            )
                                        }
                                    }
                                }
                                
                                lastPointerCount = currentPointerCount
                                
                                // Consume the event
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        
                        // Gesture ended
                        scope.launch {
                            // Sync actual values with what we've been displaying
                            scale.snapTo(zoom)
                            offset.snapTo(pan)
                            
                            if (scale.value <= 1.01f) {
                                // At minimum scale - check for swipe to dismiss
                                val velocity = velocityTracker.calculateVelocity()
                                val verticalVelocity = velocity.y
                                val verticalOffset = offset.value.y
                                
                                if (verticalOffset.absoluteValue > 200f || verticalVelocity.absoluteValue > 2000f) {
                                    // Dismiss immediately - no animation
                                    onDismiss()
                                } else {
                                    // Bounce back to center
                                    launch {
                                        scale.animateTo(
                                            1f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    launch {
                                        offset.animateTo(
                                            Offset.Zero,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            } else {
                                // Zoomed in - only apply fling if we weren't zooming
                                if (!isZooming) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    
                                    launch {
                                        // Apply fling with decay
                                        try {
                                            offset.animateDecay(
                                                initialVelocity = Offset(velocity.x, velocity.y),
                                                animationSpec = androidx.compose.animation.core.exponentialDecay(
                                                    frictionMultiplier = 2.5f,
                                                    absVelocityThreshold = 0.1f
                                                )
                                            )
                                        } catch (e: Exception) {
                                            // Animation cancelled
                                        } finally {
                                            // Ensure we're within bounds
                                            val constrained = constrainOffset(offset.value)
                                            if (constrained != offset.value) {
                                                offset.animateTo(
                                                    constrained,
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Was zooming - just ensure bounds without fling
                                    val constrained = constrainOffset(offset.value)
                                    if (constrained != offset.value) {
                                        offset.animateTo(
                                            constrained,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            scope.launch {
                                if (scale.value > 1.5f) {
                                    // Zoom out to default
                                    launch {
                                        scale.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    launch {
                                        offset.animateTo(
                                            targetValue = Offset.Zero,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                } else {
                                    // Zoom in to the tapped point
                                    val targetScale = 3f
                                    
                                    // Calculate focal point
                                    val centerX = containerSize.width / 2f
                                    val centerY = containerSize.height / 2f
                                    val focusPoint = tapOffset - Offset(centerX, centerY)
                                    
                                    // Calculate target offset to keep tap point in same screen position
                                    val targetOffset = focusPoint * (1f - targetScale)
                                    
                                    launch {
                                        scale.animateTo(
                                            targetValue = targetScale,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    launch {
                                        offset.animateTo(
                                            targetValue = targetOffset,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )
                },
        )
        if (isImageLoading) {
            HdImageLoadingIndicator(Modifier.align(Alignment.Center))
        }

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Close, "Close", tint = Color.White)
        }
    }
}
