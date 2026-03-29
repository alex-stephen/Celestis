package com.example.celestis.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.example.celestis.data.ApodRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WorkManager background worker that syncs the daily APOD.
 * 
 * Features:
 * - Fetches latest APOD from NASA API
 * - Pre-caches image using Coil3
 * - Validates date matches current date (handles timezone differences)
 * - Automatically retries if NASA hasn't published yet
 */
class ApodSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: ApodRepository by inject()
    private val imageLoader: ImageLoader by inject()

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting APOD background sync")

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
                    Log.w(TAG, "Sync failed: No APOD data received")
                    Result.retry()
                }
                latestApod.date != today -> {
                    Log.w(TAG, "NASA hasn't published today's APOD yet. Got ${latestApod.date}, expected $today")
                    Result.retry()
                }
                else -> {
                    // Pre-cache the standard resolution image
                    latestApod.url?.let { url ->
                        val request = ImageRequest.Builder(applicationContext)
                            .data(url)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                        imageLoader.enqueue(request)
                    }

                    Log.d(TAG, "Sync successful: ${latestApod.title} (${latestApod.date})")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with exception", e)
            // Retry on failure (network issues, etc.)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ApodSyncWorker"
        const val WORK_NAME = "apod_daily_sync"
    }
}
