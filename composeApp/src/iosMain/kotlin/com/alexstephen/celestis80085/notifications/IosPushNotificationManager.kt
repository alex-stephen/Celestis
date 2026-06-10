package com.alexstephen.celestis80085.notifications

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosPushNotificationManager : PushNotificationManager {

    /**
     * Presents the iOS system notification permission dialog.
     * Returns true if the user grants (or has previously granted) permission.
     * Subsequent calls after the user has already decided return the current
     * status without showing the dialog again.
     */
    override suspend fun requestPermission(): Boolean = suspendCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, _ ->
                cont.resume(granted)
            }
    }

    override suspend fun getToken(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("fcm_device_token")
}