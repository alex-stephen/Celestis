package com.alexstephen.celestis80085.ui.utils

/**
 * Utility object for generating deep links for the Celestis app.
 * 
 * Deep links follow the format: https://getcelestis.com/photo/{date}
 * where {date} is in YYYY-MM-DD format.
 * 
 * NOTE: Change BASE_URL to your actual domain when you purchase it.
 * You'll also need to update:
 * - AndroidManifest.xml (android:host)
 * - Info.plist (CFBundleURLName)
 * - iosApp.entitlements (associated-domains)
 */
object LinkGenerator {
    private const val BASE_URL = "https://getcelestis.com"
    private const val PHOTO_PATH = "photo"
    
    /**
     * Generates a deep link URL for a specific APOD photo by date.
     * 
     * @param date The date in YYYY-MM-DD format
     * @return A deep link URL in the format: https://getcelestis.com/photo/{date}
     */
    fun generatePhotoLink(date: String): String {
        return "$BASE_URL/$PHOTO_PATH/$date"
    }
    
    /**
     * Extracts the date from a deep link URL.
     * 
     * @param url The deep link URL
     * @return The date string if valid, null otherwise
     */
    fun extractDateFromLink(url: String): String? {
        return try {
            val uri = url.trim()
            // Match pattern: https://getcelestis.com/photo/YYYY-MM-DD
            val regex = Regex("$BASE_URL/$PHOTO_PATH/([0-9]{4}-[0-9]{2}-[0-9]{2})")
            val matchResult = regex.find(uri)
            matchResult?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }
}
