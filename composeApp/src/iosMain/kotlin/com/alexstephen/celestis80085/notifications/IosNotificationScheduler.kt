package com.alexstephen.celestis80085.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDateComponents
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAttachment
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

@OptIn(ExperimentalForeignApi::class)
class IosNotificationScheduler : NotificationScheduler {

    override fun scheduleApodNotification(title: String, imageDate: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // Cancel any existing pending notification before scheduling a new one
        center.removePendingNotificationRequestsWithIdentifiers(listOf(NOTIFICATION_ID))

        val content = UNMutableNotificationContent().apply {
            setTitle("Photo of the Day")
            setBody("Checkout the photo of the day! $title")
            setSound(UNNotificationSound.defaultSound())
        }

        // Attach locally cached image if available
        getImageAttachment(imageDate)?.let { attachment ->
            content.setAttachments(listOf(attachment))
        }

        // Next 10:00 AM local — iOS automatically chooses today if before 10 AM,
        // tomorrow if already past 10 AM.
        val components = NSDateComponents().apply {
            hour = 10
            minute = 0
            second = 0
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            components,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            NOTIFICATION_ID,
            content,
            trigger
        )

        center.addNotificationRequest(request) { error ->
            if (error != null) {
                NSLog("Celestis: Failed to schedule APOD notification: $error")
            } else {
                NSLog("Celestis: Scheduled APOD notification for next 10 AM: \"$title\"")
            }
        }
    }

    override fun cancelPendingNotification() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(NOTIFICATION_ID))
        NSLog("Celestis: Cancelled pending APOD notification")
    }

    /**
     * Builds a UNNotificationAttachment from the APOD image stored in the shared
     * App Group container by IosSyncManager.
     * Returns null if the image hasn't been downloaded yet.
     */
    private fun getImageAttachment(date: String): UNNotificationAttachment? {
        val fileManager = NSFileManager.defaultManager
        val containerUrl = fileManager
            .containerURLForSecurityApplicationGroupIdentifier(APP_GROUP_ID)
            ?: return null

        val imageUrl = containerUrl
            .URLByAppendingPathComponent("apod_images/apod_$date.jpg")
            ?: return null

        if (!fileManager.fileExistsAtPath(imageUrl.path ?: return null)) {
            NSLog("Celestis: No cached image for $date — notification will show without image")
            return null
        }

        return UNNotificationAttachment.attachmentWithIdentifier(
            identifier = "apod_image",
            URL = imageUrl,
            options = null,
            error = null
        )
    }

    companion object {
        private const val NOTIFICATION_ID = "celestis_daily_apod"
        private const val APP_GROUP_ID = "group.com.alexstephen.celestis80085"
    }
}