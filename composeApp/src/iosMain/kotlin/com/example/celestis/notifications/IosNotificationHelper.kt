package com.example.celestis.notifications

import com.example.celestis.KoinHelper
import com.example.celestis.data.ApodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.get
import platform.Foundation.NSLog

/**
 * Top-level Kotlin functions exported to Swift.
 *
 * Swift calls these from [CelestisAppDelegate] to bridge the FCM/APNs events
 * into the Kotlin layer without exposing Koin or coroutines to Swift.
 */

fun onFcmTokenReceived(token: String) {
    NSLog("Celestis: FCM token received, registering with backend")
    CoroutineScope(Dispatchers.Default).launch {
        try {
            val helper = KoinHelper()
            helper.get<ApodRepository>().registerDeviceToken(token, "ios")
            NSLog("Celestis: iOS device token registered successfully")
        } catch (e: Exception) {
            NSLog("Celestis: Failed to register iOS device token: ${e.message}")
        }
    }
}

/**
 * Called by CelestisAppDelegate when a data-only (silent) APNs push is received.
 * Schedules a local notification to fire at 10:00 AM in the device's local timezone.
 */
fun scheduleNotificationFromPush(title: String, date: String) {
    NSLog("Celestis: Scheduling notification from silent push — \"$title\" ($date)")
    try {
        val helper = KoinHelper()
        helper.get<NotificationScheduler>().scheduleApodNotification(title, date)
    } catch (e: Exception) {
        NSLog("Celestis: Failed to schedule notification from push: ${e.message}")
    }
}