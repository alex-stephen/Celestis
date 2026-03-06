package com.example.astrolume.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApodResponse(
    val date: String,
    val title: String? = null,
    val explanation: String? = null,
    val url: String? = null,
    val copyright: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("service_version") val serviceVersion: String? = null,
    @SerialName("hdurl") val urlHD: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null, // New
    val tags: List<String> = emptyList(),
    val averageRating: Int? = 0,
    val totalVotes: Int? = 0
)