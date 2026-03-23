package com.example.celestis.ui.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.scale
import androidx.palette.graphics.Palette
import coil3.Image
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of color extraction using Palette API.
 * Downscales bitmaps to avoid OOM errors and improve performance.
 * Converts HARDWARE bitmaps to software bitmaps for Palette compatibility.
 */
actual suspend fun extractDominantColor(image: Image): Color? = withContext(Dispatchers.Default) {
    try {
        val originalBitmap = image.toBitmap()
        
        // Convert HARDWARE bitmap to software bitmap (ARGB_8888) for Palette compatibility
        val softwareBitmap = if (originalBitmap.config == Bitmap.Config.HARDWARE) {
            originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            originalBitmap
        }
        
        // Downscale bitmap to avoid OOM and improve performance
        val scaledBitmap = if (softwareBitmap.width > 200 || softwareBitmap.height > 200) {
            val scaleFactor = 200f / maxOf(softwareBitmap.width, softwareBitmap.height)
            val newWidth = (softwareBitmap.width * scaleFactor).toInt()
            val newHeight = (softwareBitmap.height * scaleFactor).toInt()
            softwareBitmap.scale(newWidth, newHeight)
        } else {
            softwareBitmap
        }

        val palette = Palette.from(scaledBitmap).generate()
        // Clean up bitmaps
        if (scaledBitmap !== softwareBitmap) {
            scaledBitmap.recycle()
        }
        if (softwareBitmap !== originalBitmap) {
            softwareBitmap.recycle()
        }

        val selectedSwatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
        
        // Try to get dominant swatch, fallback to other swatches
        val extractedColor = selectedSwatch?.let { Color(it.rgb) }

        return@withContext extractedColor?.let { color ->
            if (isColorTooDark(color)) {
                // Boost the color to make the nebula "glow"
                boostColorVibrancy(color)
            } else {
                color
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Checks if the color is too close to black to be visible as a gradient.
 * Using HSL (Hue, Saturation, Lightness) is more accurate than RGB here.
 */
private fun isColorTooDark(color: Color): Boolean {
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        ),
        hsl
    )
    return hsl[2] < 0.2f // Lightness less than 20%
}

/**
 * Boosts the lightness and saturation of a color so it pops against the black background.
 */
private fun boostColorVibrancy(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )

    // Increase Saturation and Lightness to make the nebula "glow"
    hsv[1] = (hsv[1] + 0.3f).coerceAtMost(1.0f) // More color
    hsv[2] = (hsv[2] + 0.4f).coerceAtMost(0.8f) // More light

    return Color(android.graphics.Color.HSVToColor(hsv))
}