package com.example.celestis.sync

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.example.celestis.data.ApodRepository
import com.example.celestis.notifications.NotificationScheduler
import com.example.celestis.widget.ApodWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * WorkManager background worker that syncs the daily APOD.
 * 
 * Features:
 * - Fetches latest APOD from NASA API at 6:00 AM UTC daily
 * - Downloads image to local storage for widget access
 * - Pre-caches image using Coil3 for app usage
 * - Validates date matches current date (handles timezone differences)
 * - Automatically retries if NASA hasn't published yet
 * - Cleans up old images to prevent storage bloat
 * 
 * Widget Image Storage:
 * - Location: context.filesDir/apod_images/
 * - Naming: apod_YYYY-MM-DD.jpg
 * - Retention: Latest 7 days (configurable)
 */
class ApodSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: ApodRepository by inject()
    private val imageLoader: ImageLoader by inject()
    private val notificationScheduler: NotificationScheduler by inject()
    
    private val imageStorageDir = File(applicationContext.filesDir, APOD_IMAGES_DIR).apply {
        if (!exists()) mkdirs()
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "===== APOD SYNC STARTED =====")
            Log.d(TAG, "Sync triggered at: ${Clock.System.now().toLocalDateTime(TimeZone.UTC)}")

            // Fetch the latest APOD and cache in database
            repository.refreshLatest()

            // Validate that we got today's APOD
            val latestApod = repository.observeLatestApod().firstOrNull()
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.UTC)
                .date
                .toString()

            when {
                latestApod == null -> {
                    Log.w(TAG, "❌ Sync failed: No APOD data received")
                    Result.retry()
                }
                latestApod.date != today -> {
                    Log.w(TAG, "⏰ NASA hasn't published today's APOD yet. Got ${latestApod.date}, expected $today")
                    Result.retry()
                }
                else -> {
                    // Download image to local storage for widget access
                    val imageUrl = if (latestApod.mediaType.equals("video", ignoreCase = true)) {
                        latestApod.thumbnailUrl ?: latestApod.url
                    } else {
                        latestApod.url // Use standard resolution for widgets
                    }
                    
                    if (imageUrl != null) {
                        val success = downloadImageToLocalStorage(imageUrl, latestApod.date)
                        
                        if (success) {
                            Log.d(TAG, "✅ Successfully downloaded image for ${latestApod.date}")
                            
                            // Also pre-cache in Coil for app usage
                            precacheImageInCoil(imageUrl)
                            
                            // Clean up old images to prevent storage bloat
                            cleanupOldImages()
                            
                            // Force immediate widget update
                            Log.d(TAG, "📱 Triggering widget update...")
                            updateWidgets()
                            
                            // Fallback: if the FCM silent push was missed (device was offline,
                            // Doze mode, etc.) reschedule the local 10 AM notification here.
                            notificationScheduler.scheduleApodNotification(
                                title = latestApod.title ?: "",
                                imageDate = latestApod.date
                            )

                            // Record today's sync date so the widget receiver can skip
                            // redundant network hits on every device unlock.
                            applicationContext.getSharedPreferences(SYNC_PREFS_NAME, android.content.Context.MODE_PRIVATE)
                                .edit {
                                    putString(KEY_LAST_SYNC_DATE, latestApod.date)
                                }

                            Log.d(TAG, "===== SYNC SUCCESSFUL: ${latestApod.title} (${latestApod.date}) =====")
                            Result.success()
                        } else {
                            Log.w(TAG, "Failed to download image, will retry")
                            Result.retry()
                        }
                    } else {
                        Log.w(TAG, "No valid image URL available")
                        Result.success() // Don't retry if there's no image
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with exception", e)
            // Retry on failure (network issues, rate limits, etc.)
            Result.retry()
        }
    }

    /**
     * Downloads the APOD image to local storage for widget access.
     * Widgets cannot efficiently load from network or Coil cache, so we store
     * the image as a file with a predictable path.
     * 
     * @param url The image URL to download
     * @param date The APOD date (used for filename)
     * @return true if download succeeded, false otherwise
     */
    private suspend fun downloadImageToLocalStorage(url: String, date: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Execute the image request and get the result
                val request = ImageRequest.Builder(applicationContext)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                
                val result = imageLoader.execute(request)
                
                if (result is SuccessResult) {
                    // Get the image from Coil's disk cache
                    val diskCache = imageLoader.diskCache
                    val cacheKey = diskCache?.openSnapshot(url)
                    
                    if (cacheKey != null) {
                        try {
                            val sourceFile = cacheKey.data.toFile()
                            val outputFile = File(imageStorageDir, "apod_$date.jpg")
                            
                            // Create a high-quality widget-optimized version
                            val optimized = createWidgetOptimizedImage(sourceFile, date)
                            
                            if (optimized) {
                                Log.d(TAG, "Widget-optimized image saved to: ${outputFile.absolutePath}")
                                return@withContext true
                            } else {
                                // Fallback: just copy the original if optimization fails
                                sourceFile.copyTo(outputFile, overwrite = true)
                                Log.w(TAG, "Optimization failed, using original image")
                                return@withContext true
                            }
                        } finally {
                            cacheKey.close()
                        }
                    } else {
                        Log.w(TAG, "Image loaded but not found in disk cache")
                        return@withContext false
                    }
                } else if (result is ErrorResult) {
                    Log.e(TAG, "Failed to load image: ${result.throwable.message}")
                    return@withContext false
                }
                
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image to local storage", e)
                false
            }
        }
    }
    
    /**
     * Creates a widget-optimized version of the image with high quality settings.
     * 
     * Strategy:
     * - Target size: 800x800px (good balance of quality vs memory)
     * - Format: ARGB_8888 (full color depth)
     * - High-quality filtering during scaling
     * - JPEG quality: 90 (high quality compression)
     * 
     * Memory calculation: 800x800x4 bytes = 2.56MB per image (well under 15.5MB limit)
     * With 3 widget instances: ~7.7MB (safe margin)
     */
    private fun createWidgetOptimizedImage(sourceFile: File, date: String): Boolean {
        return try {
            // First pass: Get dimensions
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath, options)
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            val targetSize = 800 // Larger than before for better quality
            
            // Calculate sample size for initial decode
            var inSampleSize = 1
            val largerDimension = maxOf(originalWidth, originalHeight)
            
            // Decode at 2x target size, then scale down with high quality
            while (largerDimension / (inSampleSize * 2) > targetSize * 2) {
                inSampleSize *= 2
            }
            
            // Second pass: Decode with sample size
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 // Full quality
            
            val bitmap = android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath, options)
                ?: return false
            
            try {
                // Calculate final dimensions maintaining aspect ratio
                val scale = minOf(
                    targetSize.toFloat() / bitmap.width,
                    targetSize.toFloat() / bitmap.height
                )
                
                val finalWidth = (bitmap.width * scale).toInt()
                val finalHeight = (bitmap.height * scale).toInt()
                
                // High-quality scaling with filtering
                val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    finalWidth,
                    finalHeight,
                    true // High quality filtering
                )
                
                // Save with high JPEG quality
                val outputFile = File(imageStorageDir, "apod_$date.jpg")
                outputFile.outputStream().use { out ->
                    scaledBitmap.compress(
                        android.graphics.Bitmap.CompressFormat.JPEG,
                        90, // High quality
                        out
                    )
                }
                
                val sizeInKB = outputFile.length() / 1024
                Log.d(TAG, "Widget-optimized image: ${scaledBitmap.width}x${scaledBitmap.height}, " +
                        "file size: ${sizeInKB}KB (original: ${originalWidth}x${originalHeight})")
                
                // Clean up bitmaps
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                }
                scaledBitmap.recycle()
                
                true
            } catch (e: Exception) {
                bitmap.recycle()
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating widget-optimized image", e)
            false
        }
    }

    /**
     * Pre-caches the image in Coil for app usage.
     */
    private fun precacheImageInCoil(url: String) {
        val request = ImageRequest.Builder(applicationContext)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        imageLoader.enqueue(request)
    }

    /**
     * Updates all APOD widget instances with the latest data.
     * This triggers Glance to re-render all widgets on the home screen.
     * 
     * @return true if update was successful, false if there were issues
     */
    private suspend fun updateWidgets(): Boolean {
        return try {
            Log.d(TAG, "Calling ApodWidget().updateAll()...")
            ApodWidget().updateAll(applicationContext)
            Log.d(TAG, "updateAll() completed without exceptions")
            
            // Force a second update after a short delay to ensure state propagation
            kotlinx.coroutines.delay(500)
            ApodWidget().updateAll(applicationContext)
            Log.d(TAG, "Second updateAll() completed (state propagation)")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update widgets", e)
            // Non-critical error, don't fail the sync
            false
        }
    }
    
    /**
     * Cleans up images older than RETENTION_DAYS to prevent unlimited storage growth.
     * Keeps the latest 7 days of images by default.
     */
    private fun cleanupOldImages() {
        try {
            val files = imageStorageDir.listFiles() ?: return
            
            // Sort by last modified date, keep newest RETENTION_DAYS files
            val filesToDelete = files
                .sortedByDescending { it.lastModified() }
                .drop(RETENTION_DAYS)
            
            filesToDelete.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "Deleted old image: ${file.name}")
                }
            }
            
            Log.d(TAG, "Cleanup complete: ${filesToDelete.size} old images deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old images", e)
        }
    }

    companion object {
        private const val TAG = "ApodSyncWorker"
        const val WORK_NAME = "apod_daily_sync"
        const val APOD_IMAGES_DIR = "apod_images"
        private const val RETENTION_DAYS = 7 // Keep latest 7 days of images
        const val SYNC_PREFS_NAME = "celestis_sync"
        const val KEY_LAST_SYNC_DATE = "last_sync_date"
        
        /**
         * Helper method to get the local image file path for a given date.
         * Use this method in widgets to load the cached image.
         */
        fun getLocalImagePath(context: Context, date: String): File {
            return File(context.filesDir, "$APOD_IMAGES_DIR/apod_$date.jpg")
        }
    }
}
