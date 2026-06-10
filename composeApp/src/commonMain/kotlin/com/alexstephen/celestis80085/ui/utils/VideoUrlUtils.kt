package com.alexstephen.celestis80085.ui.utils

/**
 * Utility functions for handling video URLs, especially YouTube links
 */
object VideoUrlUtils {
    
    /**
     * Extracts YouTube video ID from various YouTube URL formats
     * Supports:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://www.youtube.com/embed/VIDEO_ID
     */
    fun extractYouTubeId(url: String): String? {
        val patterns = listOf(
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([^&?/]+)".toRegex(),
            "youtube\\.com/embed/([^?]+)".toRegex()
        )
        
        for (pattern in patterns) {
            pattern.find(url)?.groupValues?.getOrNull(1)?.let {
                return it
            }
        }
        return null
    }
    
    /**
     * Gets high-quality thumbnail URL for a YouTube video
     */
    fun getYouTubeThumbnail(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }
    
    /**
     * Normalizes video URL for playback
     * Converts YouTube embed URLs to direct watch URLs if needed
     */
    fun normalizeVideoUrl(url: String): String {
        val videoId = extractYouTubeId(url)
        return if (videoId != null) {
            "https://www.youtube.com/watch?v=$videoId"
        } else {
            url
        }
    }
    
    /**
     * Checks if a URL is a YouTube video
     */
    fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }
}
