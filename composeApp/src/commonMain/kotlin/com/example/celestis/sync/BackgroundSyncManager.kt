package com.example.celestis.sync

/**
 * Platform-agnostic interface for scheduling background APOD syncs.
 * 
 * Implementations:
 * - Android: Uses WorkManager for periodic background tasks
 * - iOS: Uses BGTaskScheduler for app refresh tasks
 */
interface BackgroundSyncManager {
    /**
     * Schedules a daily background sync that:
     * 1. Fetches the latest APOD from NASA API
     * 2. Pre-caches the image using Coil3
     * 3. Updates the local database
     * 4. Validates that fetched APOD date matches current date
     */
    fun scheduleDailySync()
    
    /**
     * Cancels all scheduled background sync tasks.
     * Useful for testing or when user disables background sync in settings.
     */
    fun cancelSync()
}
