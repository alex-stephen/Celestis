@file:SuppressLint("RestrictedApi")

package com.example.celestis.widget

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.celestis.MainActivity
import java.io.File

/**
 * Main composable content for the APOD widget.
 * This function determines which layout to show based on the widget size and state.
 */
@Composable
fun ApodWidgetContent() {
    val state = currentState<ApodWidgetState>()
    val size = LocalSize.current
    
    // Determine if we should show the compact layout (small widget)
    // Small widgets are typically < 150dp in height
    val isSmall = size.height < 150.dp
    
    when (state) {
        is ApodWidgetState.Success -> {
            if (isSmall) {
                SmallWidgetLayout(state)
            } else {
                MediumLargeWidgetLayout(state)
            }
        }
        is ApodWidgetState.Loading -> {
            LoadingLayout()
        }
        is ApodWidgetState.NoData -> {
            NoDataLayout()
        }
    }
}

/**
 * Small widget layout - Shows only the APOD image.
 * Optimized for minimal screen space (e.g., 2x2 grid).
 */
@Composable
private fun SmallWidgetLayout(state: ApodWidgetState.Success) {
    val context = LocalContext.current
    val bitmap = loadBitmapFromFile(context, state.imagePath)
    
    val componentName = ComponentName(context, MainActivity::class.java)
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .clickable(onClick = actionStartActivity(componentName))
    ) {
        if (bitmap != null) {
            // Load image from local file storage
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "APOD: ${state.title}",
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            // Fallback if image can't be loaded
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌌",
                    style = TextStyle(fontSize = 32.sp)
                )
            }
        }
    }
}

/**
 * Medium/Large widget layout - Shows APOD image, title, and date.
 * Provides more context for users with larger widgets (e.g., 4x2 or 4x3 grid).
 */
@SuppressLint("RestrictedApi")
@Composable
private fun MediumLargeWidgetLayout(state: ApodWidgetState.Success) {
    val context = LocalContext.current
    val bitmap = loadBitmapFromFile(context, state.imagePath)
    
    val componentName = ComponentName(context, MainActivity::class.java)
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .clickable(onClick = actionStartActivity(componentName))
    ) {
        if (bitmap != null) {
            // Background image
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "APOD: ${state.title}",
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        }
        
        // Title overlay at the bottom
        Column(
            modifier = GlanceModifier
                .fillMaxSize(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Push content to bottom
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            // Text content with semi-transparent background
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = state.title,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Date
                Text(
                    text = state.date,
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.9f)),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

/**
 * Loading layout - Shown when widget is initializing or waiting for sync.
 */
@Composable
private fun LoadingLayout() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.padding(16.dp)
        ) {
            Text(
                text = "🌌",
                style = TextStyle(fontSize = 32.sp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Loading APOD...",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp
                )
            )
        }
    }
}

/**
 * No data layout - Shown when no APOD is cached yet.
 * Prompts user to open the app.
 */
@Composable
private fun NoDataLayout() {
    val context = LocalContext.current
    val componentName = ComponentName(context, MainActivity::class.java)
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(onClick = actionStartActivity(componentName)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.padding(16.dp)
        ) {
            Text(
                text = "⭐",
                style = TextStyle(fontSize = 32.sp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Open Celestis",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "to sync your first APOD",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        }
    }
}

/**
 * Helper function to load a pre-optimized widget image from local storage.
 * 
 * Images are pre-processed during background sync with optimal settings:
 * - Size: ~800x800px (aspect ratio preserved)
 * - Format: High-quality ARGB_8888
 * - Quality: JPEG 90%
 * - Memory: ~2.56MB per image (well under 15.5MB limit)
 * 
 * This function simply loads the pre-optimized image without additional
 * processing, ensuring maximum quality on the widget.
 */
private fun loadBitmapFromFile(context: Context, filePath: String): android.graphics.Bitmap? {
    return try {
        val file = File(filePath)
        if (!file.exists()) {
            android.util.Log.w("ApodWidgetContent", "Image file not found: $filePath")
            return null
        }
        
        // Check file size first (safety check)
        val fileSizeKB = file.length() / 1024
        
        // Load the pre-optimized image with high quality settings
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 // Full color depth
            inSampleSize = 1 // No downsampling - images are pre-optimized
        }
        
        val bitmap = BitmapFactory.decodeFile(filePath, options)
        
        if (bitmap != null) {
            val memoryKB = bitmap.byteCount / 1024
            android.util.Log.d("ApodWidgetContent", 
                "Loaded widget image: ${bitmap.width}x${bitmap.height}, " +
                "memory: ${memoryKB}KB, file: ${fileSizeKB}KB")
            
            // Safety check: If image is unexpectedly large, scale it down
            if (bitmap.byteCount > 4 * 1024 * 1024) { // > 4MB
                android.util.Log.w("ApodWidgetContent", 
                    "Image larger than expected (${memoryKB}KB), scaling down for safety")
                
                val scale = 0.75f // Scale down by 25%
                val scaledBitmap = bitmap.scale(
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt()
                )
                
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                }
                return scaledBitmap
            }
        }
        
        bitmap
    } catch (e: Exception) {
        android.util.Log.e("ApodWidgetContent", "Error loading bitmap for widget", e)
        null
    }
}
