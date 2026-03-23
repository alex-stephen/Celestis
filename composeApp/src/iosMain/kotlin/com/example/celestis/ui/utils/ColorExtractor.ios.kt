package com.example.celestis.ui.utils

import androidx.compose.ui.graphics.Color
import coil3.Image

/**
 * iOS implementation of color extraction.
 * Currently returns null - can be implemented using CoreImage or similar iOS APIs if needed.
 */
actual suspend fun extractDominantColor(image: Image): Color? {
    // TODO: Implement iOS color extraction if needed
    // Could use CoreImage or other iOS-specific APIs
    return null
}
