package com.alexstephen.celestis80085.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Android implementation of BackgroundSyncManager using WorkManager.
 * 
 * Schedules a periodic daily sync at exactly 5:00 AM UTC with:
 * - 24-hour repeat interval
 * - Network connectivity requirement
 * - Small jitter (5-10 min) to prevent thundering herd
 * - Exponential backoff for failures
 * 
 * NASA APOD publishes at 00:00 Eastern (04:00-05:00 UTC depending on DST).
 * 5:00 AM UTC scheduling provides immediate availability after publish.
 */
class AndroidSyncManager(
    private val context: Context
) : BackgroundSyncManager {

    private val workManager = WorkManager.getInstance(context)

    override fun scheduleDailySync() {
        // Calculate delay until next 5:00 AM UTC
        val initialDelayMinutes = calculateDelayUntilFiveAmUtc()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<ApodSyncWorker>(
            repeatInterval = 24, // Once per day
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, // Initial backoff delay
                TimeUnit.SECONDS
            )
            .build()

        // Use KEEP policy to avoid rescheduling if already scheduled
        workManager.enqueueUniquePeriodicWork(
            ApodSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        Log.d(TAG, "Scheduled daily APOD sync at 5:00 AM UTC (initial delay: ${initialDelayMinutes}min)")
    }

    /**
     * Calculates the delay in minutes from now until the next 5:00 AM UTC.
     * Adds 5-10 minute jitter to prevent all users hitting NASA API simultaneously.
     * 
     * Example:
     * - Current time: 2026-03-29 20:00 UTC
     * - Next 5:00 AM UTC: 2026-03-30 05:00 UTC
     * - Delay: 9 hours + jitter
     */
    private fun calculateDelayUntilFiveAmUtc(): Long {
        val nowUtc = Clock.System.now()
        val nowLocalUtc = nowUtc.toLocalDateTime(TimeZone.UTC)
        
        // Target: 5:00 AM UTC
        val todayFiveAmUtc = nowLocalUtc.date.atTime(5, 0).toInstant(TimeZone.UTC)
        
        // If it's already past 5:00 AM UTC today, target tomorrow
        val nextFiveAmUtc = if (nowUtc >= todayFiveAmUtc) {
            nowLocalUtc.date.plus(1, DateTimeUnit.DAY).atTime(5, 0).toInstant(TimeZone.UTC)
        } else {
            todayFiveAmUtc
        }
        
        // Calculate base delay in minutes
        val baseDelayMinutes = nowUtc.until(nextFiveAmUtc, DateTimeUnit.MINUTE)
        
        // Add small jitter (5-10 minutes) to prevent thundering herd
        val jitterMinutes = Random.nextLong(5, 11)
        
        val totalDelay = baseDelayMinutes + jitterMinutes
        
        Log.d(TAG, "Current: ${nowLocalUtc}, Target: ${nextFiveAmUtc.toLocalDateTime(TimeZone.UTC)}, " +
                "Delay: ${baseDelayMinutes}min + ${jitterMinutes}min jitter = ${totalDelay}min")
        
        return totalDelay
    }

    override fun cancelSync() {
        workManager.cancelUniqueWork(ApodSyncWorker.WORK_NAME)
        Log.d(TAG, "Cancelled daily APOD sync")
    }

    companion object {
        private const val TAG = "AndroidSyncManager"
    }
}
