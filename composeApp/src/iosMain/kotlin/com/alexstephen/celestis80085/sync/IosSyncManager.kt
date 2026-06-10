package com.alexstephen.celestis80085.sync

import coil3.ImageLoader
import coil3.PlatformContext
import com.alexstephen.celestis80085.notifications.NotificationScheduler
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.alexstephen.celestis80085.BuildKonfig
import com.alexstephen.celestis80085.data.ApodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataTaskWithURL
import platform.Foundation.dateByAddingTimeInterval
import platform.Foundation.writeToURL
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/**
 * iOS implementation of BackgroundSyncManager using BGTaskScheduler.
 *
 * After a successful sync this class:
 *  1. Writes APOD metadata to the shared App Group UserDefaults so the widget
 *     can read the title / date / explanation without launching the main app.
 *  2. Downloads the APOD image into the shared App Group file container so
 *     the widget extension (which runs in a separate process) can load it.
 *
 * Both the main app target and the CelestisWidget extension must declare the
 * App Group "group.com.alexstephen.celestis80085" in their entitlements files.
 *
 * Note: The actual task handler registration must be done in Swift's AppDelegate
 * or SceneDelegate, as it requires registering before app finishes launching.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSyncManager(
    private val repository: ApodRepository,
    private val imageLoader: ImageLoader,
    private val context: PlatformContext,
    private val notificationScheduler: NotificationScheduler
) : BackgroundSyncManager {

    override fun scheduleDailySync() {
        val request = BGAppRefreshTaskRequest(TASK_IDENTIFIER)
        request.earliestBeginDate =
            platform.Foundation.NSDate().dateByAddingTimeInterval(24.0 * 60.0 * 60.0)

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
     * Core sync logic called by the Swift background-task handler.
     * Returns true when today's APOD has been fetched, cached, and the widget
     * data has been written to the shared App Group.
     */
    suspend fun performSync(): Boolean {
        return try {
            NSLog("Celestis: Starting APOD background sync")

            // Fetch the latest APOD and save it to the local SQLDelight database.
            repository.refreshLatest()

            // Validate that we received today's APOD.
            val latestApod = repository.observeLatestApod().firstOrNull()
            val now = Clock.System.now()
            val today = now
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()

            when {
                latestApod == null -> {
                    NSLog("Celestis: Sync failed – no APOD data received")
                    false
                }

                latestApod.date != today -> {
                    NSLog(
                        "Celestis: NASA hasn't published today's APOD yet. " +
                                "Got ${latestApod.date}, expected $today"
                    )
                    false
                }

                else -> {
                    // Pre-cache via Coil for in-app image display.
                    val displayUrl = when {
                        latestApod.mediaType.equals("video", ignoreCase = true) ->
                            latestApod.thumbnailUrl ?: latestApod.url
                        else -> latestApod.url
                    }

                    displayUrl?.let { url ->
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                        imageLoader.enqueue(request)
                    }

                    // Write metadata to the shared App Group so the widget extension
                    // can read it without spinning up the main app.
                    writeApodMetadataToAppGroup(
                        title = latestApod.title ?: "Astronomy Picture of the Day",
                        date = latestApod.date,
                        explanation = latestApod.explanation
                    )

                    // Download the image into the shared App Group container.
                    displayUrl?.let { url ->
                        downloadImageToAppGroup(url = url, date = latestApod.date)
                    }

                    // Fallback: reschedule the 10 AM local notification in case the
                    // FCM silent push was missed while the device was offline.
                    notificationScheduler.scheduleApodNotification(
                        title = latestApod.title ?: "",
                        imageDate = latestApod.date
                    )

                    NSLog("Celestis: Sync successful – ${latestApod.title} (${latestApod.date})")
                    true
                }
            }
        } catch (e: Exception) {
            NSLog("Celestis: Sync failed with exception – ${e.message}")
            false
        }
    }

    /**
     * Non-suspend entry point for the Swift background task handler.
     * Launches a coroutine internally and invokes [onComplete] when done.
     */
    fun startBackgroundSync(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val success = performSync()
            onComplete(success)
        }
    }

    // -------------------------------------------------------------------------
    // App Group helpers
    // -------------------------------------------------------------------------

    /**
     * Writes APOD metadata (title, date, explanation, API base URL) to the
     * shared UserDefaults suite so the widget extension can read them.
     */
    private fun writeApodMetadataToAppGroup(
        title: String,
        date: String,
        explanation: String?
    ) {
        val defaults = NSUserDefaults(suiteName = APP_GROUP_ID) ?: run {
            NSLog("Celestis: Could not open App Group UserDefaults – check entitlements")
            return
        }
        defaults.setObject(title, forKey = "apod_title")
        defaults.setObject(date, forKey = "apod_date")
        explanation?.let { defaults.setObject(it, forKey = "apod_explanation") }
        // Store the API base URL so the widget extension can make network calls
        // when it refreshes its timeline and no cached data is available.
        defaults.setObject(BuildKonfig.BASE_URL, forKey = "apod_api_base_url")
        defaults.synchronize()
        NSLog("Celestis: APOD metadata written to App Group ($date)")
    }

    /**
     * Downloads [url] and saves the result to
     * `<AppGroupContainer>/apod_images/apod_<date>.jpg`
     * so the widget extension process can load it via [FileManager].
     */
    private suspend fun downloadImageToAppGroup(url: String, date: String) {
        withContext(Dispatchers.IO) {
            try {
                val fileManager = NSFileManager.defaultManager
                val containerURL = fileManager
                    .containerURLForSecurityApplicationGroupIdentifier(APP_GROUP_ID)
                    ?: run {
                        NSLog("Celestis: App Group container not found – check entitlements")
                        return@withContext
                    }

                // Ensure the images sub-directory exists.
                val imagesDirURL = containerURL.URLByAppendingPathComponent("apod_images")
                    ?: return@withContext
                fileManager.createDirectoryAtURL(
                    imagesDirURL,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )

                // Download the image bytes synchronously (we are already on IO dispatcher).
                val nsUrl = NSURL.URLWithString(url) ?: run {
                    NSLog("Celestis: Invalid image URL: $url")
                    return@withContext
                }
                // Download image synchronously using NSURLSession on the IO dispatcher.
                val imageFileURL = imagesDirURL.URLByAppendingPathComponent("apod_$date.jpg")
                    ?: return@withContext

                val semaphore = dispatch_semaphore_create(0)
                var downloadedData: NSData? = null

                NSURLSession.sharedSession.dataTaskWithURL(nsUrl) { data, _, error ->
                    if (error != null) {
                        NSLog("Celestis: Image download error: ${error.localizedDescription}")
                    } else {
                        downloadedData = data
                    }
                    dispatch_semaphore_signal(semaphore)
                }.resume()

                dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

                val imageData = downloadedData ?: run {
                    NSLog("Celestis: Image download returned nil for $url")
                    return@withContext
                }

                val written = imageData.writeToURL(imageFileURL, atomically = true)

                if (written) {
                    NSLog("Celestis: Image saved to App Group – apod_$date.jpg (${imageData.length} bytes)")
                } else {
                    NSLog("Celestis: Failed to write image to App Group")
                }
            } catch (e: Exception) {
                NSLog("Celestis: Exception while downloading image to App Group – ${e.message}")
            }
        }
    }

    companion object {
        const val TASK_IDENTIFIER = "com.alexstephen.celestis80085.refresh"
        private const val APP_GROUP_ID = "group.com.alexstephen.celestis80085"
    }
}