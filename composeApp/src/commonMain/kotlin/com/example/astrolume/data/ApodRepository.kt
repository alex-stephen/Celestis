package com.example.astrolume.data

import com.example.astrolume.database.ApodEntity
import com.example.astrolume.database.AppDatabase
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.service.NasaApi
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ApodRepository(
    private val api: NasaApi,
    private val database: AppDatabase
) {
    private val queries = database.appDatabaseQueries

    suspend fun fetchApod(date: String): ApodEntity {
        // 1. Local check
        val cached = queries.getApodByDate(date).executeAsOneOrNull()
        if (cached != null) return cached

        // 2. Network fetch (Now using our optimized Proxy Server)
        val remote = api.getApodFromServer(date)

        // 3. Persist to SQL (No UUID generation needed!)
        queries.insertApod(
            date = remote.date,
            explanation = remote.explanation,
            mediaType = remote.mediaType,
            serviceVersion = remote.serviceVersion,
            title = remote.title,
            urlHD = remote.urlHD,
            url = remote.url,
            thumbnailUrl = remote.thumbnailUrl,
            tags = remote.tags.toJsonString(),// List<String> -> JSON String
            copyright = remote.copyright,
            isFavorite = false,
            createdAt = Clock.System.now().toString()
        )

        return queries.getApodByDate(date).executeAsOne()
    }

    suspend fun searchByTag(tag: String): List<ApodResponse> {
        // Search usually bypasses local cache to get fresh results from the 100k+ global DB
        return api.searchByTag(tag)
    }

    // Extensions for cleaner Repository code
    fun ApodEntity.getTagsList(): List<String> {
        return try {
            Json.decodeFromString<List<String>>(this.tags ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun List<String>?.toJsonString(): String {
        return Json.encodeToString<List<String>>(this ?: emptyList())
    }
}
