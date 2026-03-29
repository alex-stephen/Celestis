package com.example.celestis.sync

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.example.celestis.data.ApodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSLog
import platform.Foundation.dateByAddingTimeInterval

/**
 * iOS implementation of BackgroundSyncManager using BGTaskScheduler.
 * 
 * Note: The actual task handler registration must be done in Swift's AppDelegate
 * or SceneDelegate, as it requires registering before app finishes launching.
 * This class provides the Kotlin logic that Swift will invoke.
 */
class IosSyncManager(
    private val repository: ApodRepository,
    private val imageLoader: ImageLoader,
    private val context: PlatformContext
) : BackgroundSyncManager {

    override fun scheduleDailySync() {
        val request = BGAppRefreshTaskRequest(TASK_IDENTIFIER)
        request.earliestBeginDate = platform.Foundation.NSDate().dateByAddingTimeInterval(24.0 * 60.0 * 60.0)
        
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
            NSLog("Celestis: Scheduled daily APOD sync")
        } catch (e: Exception) {
            NSLog("Celestis: Failed to schedule background sync: ${e.message}")
        }
    }

    override fun cancelSync() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(TASK_IDENTIFIER)
        NSLog("Celestis: Cancelled daily APOD sync")
    }

    /**
     * This is the actual sync logic that will be called by the Swift background task handler.
     * Returns true if sync succeeded, false if it should retry.
     */
    suspend fun performSync(): Boolean {
        return try {
            NSLog("Celestis: Starting APOD background sync")

            // Fetch the latest APOD and pre-cache image
            repository.refreshLatest()

            // Validate that we got today's APOD
            val latestApod = repository.observeLatestApod().firstOrNull()
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()

            when {
                latestApod == null -> {
                    NSLog("Celestis: Sync failed - No APOD data received")
                    false
                }
                latestApod.date != today -> {
                    NSLog("Celestis: NASA hasn't published today's APOD yet. Got ${latestApod.date}, expected $today")
                    false
                }
                else -> {
                    // Pre-cache the standard resolution image
                    latestApod.url?.let { url ->
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                        imageLoader.enqueue(request)
                    }

                    NSLog("Celestis: Sync successful - ${latestApod.title} (${latestApod.date})")
                    true
                }
            }
        } catch (e: Exception) {
            NSLog("Celestis: Sync failed with exception - ${e.message}")
            false
        }
    }

    companion object {
        const val TASK_IDENTIFIER = "com.example.celestis.refresh"
        
        /**
         * Helper function to be called from Swift background task handler.
         * This bridges the Swift async world to Kotlin coroutines.
         */
        fun handleBackgroundTask(
            syncManager: IosSyncManager,
            onComplete: (Boolean) -> Unit
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                val success = syncManager.performSync()
                onComplete(success)
            }
        }
    }
}
