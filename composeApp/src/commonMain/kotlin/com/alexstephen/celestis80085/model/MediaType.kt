package com.alexstephen.celestis80085.model

enum class MediaType {
    IMAGE, VIDEO, UNKNOWN;
    
    companion object {
        fun fromString(type: String?): MediaType {
            return when(type?.lowercase()) {
                "image" -> IMAGE
                "video" -> VIDEO
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Extension functions for ApodResponse to handle media type logic
 */
fun ApodResponse.getDisplayUrl(): String? {
    return when (MediaType.fromString(mediaType)) {
        MediaType.IMAGE -> url
        MediaType.VIDEO -> thumbnailUrl ?: url // Fallback to url for placeholder logic
        MediaType.UNKNOWN -> url
    }
}

fun ApodResponse.isVideo(): Boolean = 
    MediaType.fromString(mediaType) == MediaType.VIDEO

fun ApodResponse.isImage(): Boolean = 
    MediaType.fromString(mediaType) == MediaType.IMAGE
