package com.alexstephen.celestis80085.notifications

interface PushNotificationManager {
    /**
     * Android: returns current permission status (dialog is triggered from MainActivity).
     * iOS: shows the system permission dialog and returns the result.
     */
    suspend fun requestPermission(): Boolean

    /**
     * Returns the FCM device token, or null if unavailable.
     */
    suspend fun getToken(): String?
}