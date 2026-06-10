package com.alexstephen.celestis80085.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import coil3.BitmapImage
import coil3.Image
import org.jetbrains.skia.Image as SkiaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * iOS implementation of dominant color extraction.
 * Uses Compose Multiplatform's ImageBitmap.toPixelMap() to sample pixels
 * directly — no platform-specific APIs required.
 */
actual suspend fun extractDominantColor(image: Image): Color? = withContext(Dispatchers.Default) {
    try {
        val bitmapImage = image as? BitmapImage ?: return@withContext null
        val imageBitmap = SkiaImage.makeFromBitmap(bitmapImage.bitmap).toComposeImageBitmap()

        val width = imageBitmap.width
        val height = imageBitmap.height

        if (width <= 0 || height <= 0) return@withContext null

        // Sample every Nth pixel to keep it fast (target ~50 samples per axis)
        val step = max(1, max(width, height) / 50)
        val pixelMap = imageBitmap.toPixelMap()

        var rSum = 0.0
        var gSum = 0.0
        var bSum = 0.0
        var count = 0

        // Track the most "vibrant" candidate
        var bestVibrancy = -1f
        var bestColor: Color? = null

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val color = pixelMap[x, y]

                // Skip near-black and near-white pixels — they skew the result
                val brightness = (color.red + color.green + color.blue) / 3f
                if (brightness < 0.05f || brightness > 0.95f) {
                    x += step
                    continue
                }

                rSum += color.red
                gSum += color.green
                bSum += color.blue
                count++

                // Track vibrancy: distance from grey = saturation proxy
                val maxC = max(color.red, max(color.green, color.blue))
                val minC = min(color.red, min(color.green, color.blue))
                val saturation = if (maxC > 0f) (maxC - minC) / maxC else 0f
                val vibrancy = saturation * brightness

                if (vibrancy > bestVibrancy) {
                    bestVibrancy = vibrancy
                    bestColor = color
                }

                x += step
            }
            y += step
        }

        if (count == 0) return@withContext null

        // Use the most vibrant color if it's clearly vibrant, otherwise use the average
        val result = if (bestVibrancy > 0.25f && bestColor != null) {
            bestColor!!
        } else {
            Color(
                red = (rSum / count).toFloat(),
                green = (gSum / count).toFloat(),
                blue = (bSum / count).toFloat()
            )
        }

        // Apply vibrancy boost so the glow pops against the black background
        return@withContext boostColorVibrancy(result)

    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Boosts saturation and lightness of a color so it glows against a black background.
 * Pure Kotlin implementation — no android.graphics APIs used.
 */
private fun boostColorVibrancy(color: Color): Color {
    val r = color.red
    val g = color.green
    val b = color.blue

    // RGB → HSL
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC

    val lightness = (maxC + minC) / 2f

    val saturation = if (delta == 0f) 0f else delta / (1f - abs(2f * lightness - 1f))

    val hue = when {
        delta == 0f -> 0f
        maxC == r   -> 60f * (((g - b) / delta) % 6f)
        maxC == g   -> 60f * (((b - r) / delta) + 2f)
        else        -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    // Boost saturation and lightness so the colour pops on a black background
    val newSaturation = (saturation + 0.3f).coerceIn(0f, 1f)
    val newLightness  = (lightness  + 0.2f).coerceIn(0.2f, 0.8f)

    return hslToColor(hue, newSaturation, newLightness)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c  = (1f - abs(2f * l - 1f)) * s
    val x  = c * (1f - abs((h / 60f) % 2f - 1f))
    val m  = l - c / 2f

    val (r1, g1, b1) = when {
        h < 60f  -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else     -> Triple(c, 0f, x)
    }

    return Color(
        red   = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue  = (b1 + m).coerceIn(0f, 1f)
    )
}
