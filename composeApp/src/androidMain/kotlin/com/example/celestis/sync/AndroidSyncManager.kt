package com.example.celestis.sync

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
 * Schedules a periodic daily sync at exactly 6:00 AM UTC with:
 * - 24-hour repeat interval
 * - Network connectivity requirement
 * - Battery not low constraint
 * - Small jitter (5-10 min) to prevent thundering herd
 * - Exponential backoff for failures
 * 
 * NASA APOD publishes at 00:00 Eastern (04:00-05:00 UTC depending on DST).
 * 6:00 AM UTC scheduling provides 1-2 hour buffer for reliable data availability.
 */
class AndroidSyncManager(
    private val context: Context
) : BackgroundSyncManager {

    private val workManager = WorkManager.getInstance(context)

    override fun scheduleDailySync() {
        // Calculate delay until next 6:00 AM UTC
        val initialDelayMinutes = calculateDelayUntilSixAmUtc()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
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

        Log.d(TAG, "Scheduled daily APOD sync at 6:00 AM UTC (initial delay: ${initialDelayMinutes}min)")
    }

    /**
     * Calculates the delay in minutes from now until the next 6:00 AM UTC.
     * Adds 5-10 minute jitter to prevent all users hitting NASA API simultaneously.
     * 
     * Example:
     * - Current time: 2026-03-29 20:00 UTC
     * - Next 6:00 AM UTC: 2026-03-30 06:00 UTC
     * - Delay: 10 hours + jitter
     */
    private fun calculateDelayUntilSixAmUtc(): Long {
        val nowUtc = Clock.System.now()
        val nowLocalUtc = nowUtc.toLocalDateTime(TimeZone.UTC)
        
        // Target: 6:00 AM UTC
        val todaySixAmUtc = nowLocalUtc.date.atTime(6, 0).toInstant(TimeZone.UTC)
        
        // If it's already past 6:00 AM UTC today, target tomorrow
        val nextSixAmUtc = if (nowUtc >= todaySixAmUtc) {
            nowLocalUtc.date.plus(1, DateTimeUnit.DAY).atTime(6, 0).toInstant(TimeZone.UTC)
        } else {
            todaySixAmUtc
        }
        
        // Calculate base delay in minutes
        val baseDelayMinutes = nowUtc.until(nextSixAmUtc, DateTimeUnit.MINUTE)
        
        // Add small jitter (5-10 minutes) to prevent thundering herd
        val jitterMinutes = Random.nextLong(5, 11)
        
        val totalDelay = baseDelayMinutes + jitterMinutes
        
        Log.d(TAG, "Current: ${nowLocalUtc}, Target: ${nextSixAmUtc.toLocalDateTime(TimeZone.UTC)}, " +
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
