package com.alexstephen.celestis80085.ui.utils

import androidx.compose.ui.graphics.Color
import coil3.Image

/**
 * Extracts the dominant color from a Coil3 Image.
 * Returns null if extraction fails.
 */
expect suspend fun extractDominantColor(image: Image): Color?
