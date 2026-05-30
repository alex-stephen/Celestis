package com.example.celestis.notifications

import android.util.Log
import com.example.celestis.data.ApodRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Receives FCM messages on Android.
 *
 * Two entry points:
 * - [onNewToken]: Firebase rotated the device token — re-register with the backend.
 * - [onMessageReceived]: A data-only (silent) push arrived from the backend at ~05:15 UTC
 *   containing today's APOD title. We schedule a local AlarmManager notification to fire
 *   at 10:00 AM local time so the user sees it at a sensible hour.
 */
class CelestisFcmService : FirebaseMessagingService(), KoinComponent {

    private val repository: ApodRepository by inject()
    private val notificationScheduler: NotificationScheduler by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed")
        serviceScope.launch {
            try {
                repository.registerDeviceToken(token, "android")
                Log.d(TAG, "Device token registered with backend")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register device token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] != "daily_apod") return

        val title = message.data["apod_title"] ?: run {
            Log.w(TAG, "Received daily_apod push with no title — ignoring")
            return
        }
        val date = message.data["apod_date"] ?: run {
            Log.w(TAG, "Received daily_apod push with no date — ignoring")
            return
        }

        Log.d(TAG, "Received daily APOD push: \"$title\" ($date)")
        notificationScheduler.scheduleApodNotification(title, date)
    }

    companion object {
        private const val TAG = "CelestisFcmService"
    }
}
