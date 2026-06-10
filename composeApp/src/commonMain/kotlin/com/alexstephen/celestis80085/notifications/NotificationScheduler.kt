package com.alexstephen.celestis80085.notifications

interface NotificationScheduler {
    /**
     * Schedules (or reschedules) a local notification to fire at 10:00 AM in the
     * device's local timezone. If the current time is already past 10 AM, the
     * notification is scheduled for the following day.
     *
     * @param title     The APOD title to display in the notification body.
     * @param imageDate The APOD date string (YYYY-MM-DD) used to locate the cached image.
     */
    fun scheduleApodNotification(title: String, imageDate: String)

    fun cancelPendingNotification()
}