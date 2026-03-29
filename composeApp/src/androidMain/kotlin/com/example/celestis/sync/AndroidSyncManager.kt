package com.example.celestis.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Android implementation of BackgroundSyncManager using WorkManager.
 * 
 * Schedules a periodic daily sync with:
 * - 24-hour repeat interval
 * - Network connectivity requirement
 * - Battery not low constraint
 * - Random initial delay (jitter) to prevent thundering herd
 */
class AndroidSyncManager(
    private val context: Context
) : BackgroundSyncManager {

    private val workManager = WorkManager.getInstance(context)

    override fun scheduleDailySync() {
        // Add randomized jitter (0-60 minutes) to prevent all users hitting API at midnight
        val jitterMinutes = Random.nextLong(0, 61)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<ApodSyncWorker>(
            repeatInterval = 24, // Once per day
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(jitterMinutes, TimeUnit.MINUTES) // Thundering herd prevention
            .build()

        // Use KEEP policy to avoid rescheduling if already scheduled
        workManager.enqueueUniquePeriodicWork(
            ApodSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        Log.d(TAG, "Scheduled daily APOD sync with ${jitterMinutes}min initial delay")
    }

    override fun cancelSync() {
        workManager.cancelUniqueWork(ApodSyncWorker.WORK_NAME)
        Log.d(TAG, "Cancelled daily APOD sync")
    }

    companion object {
        private const val TAG = "AndroidSyncManager"
    }
}
