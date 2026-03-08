package com.example.astrolume.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.astrolume.Platform
import com.example.astrolume.database.ApodEntity
import com.example.astrolume.database.AppDatabase
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.service.NasaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ApodRepository(
    private val api: NasaApi,
    private val database: AppDatabase,
    private val imageLoader: ImageLoader,
    private val platform: Platform
) {
    private val queries = database.appDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns a Flow that emits the cached version immediately,
     * then fetches and updates from network.
     */
    fun observeLatestApod(): Flow<ApodEntity?> =
        queries.getLatestApod()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .onStart {
                // Trigger a background refresh without blocking the initial cache emission
                refreshLatest()
            }

    private suspend fun refreshLatest() {
        try {
            val remote = api.getApodFromServer(null)
            saveToLocal(remote)
        } catch (e: Exception) {
            // Log to Sentry/Crashlytics, but don't crash the Flow
        }
    }

    suspend fun fetchApod(date: String? = null): ApodEntity {
        // 1. Local check
        if (date != null) {
            val cached = queries.getApodByDate(date).executeAsOneOrNull()
            if (cached != null) return cached
        }

        // 2. Network fetch (Now using our optimized Proxy Server)
        val remote = api.getApodFromServer(date)

        // 3. Persist to SQL (No UUID generation needed!)
        saveToLocal(remote)

        return queries.getApodByDate(remote.date).executeAsOne()
    }

    /**
     * Gets random APODs. We save them locally as we get them to
     * populate the user's "Discovery" cache.
     */
    suspend fun fetchRandom(count: Int): List<ApodResponse> {
        val remotes = api.getRandomApods(count)

        // Batch Insert: Much faster and safer for the DB
        database.transaction {
            remotes.forEach { apod ->
                saveToLocal(apod)
                precacheImage(apod.url)
            }
        }
        return remotes
    }

    /**
     * Fills a specific date range. Perfect for a "Calendar" view.
     */
    suspend fun fetchRange(start: String, end: String): List<ApodResponse> {
        val remotes = api.getApodRange(start, end)

        database.transaction {
            remotes.forEach { apod ->
                saveToLocal(apod)
            }
        }
        return remotes
    }

    /**
     * Executes the Atlas Search.
     * Note: We don't save these to Local DB immediately because
     * search results are "Thin" (missing explanations).
     */
    suspend fun search(query: String): List<ApodResponse> {
        return api.searchAllFields(query)
    }

    private fun precacheImage(url: String?) {
        val request = ImageRequest.Builder(platform.context) // platformContext provided via KMP
            .data(url)
            .crossfade(true)
            .build()

        imageLoader.enqueue(request)
    }

    private fun saveToLocal(remote: ApodResponse) {
        database.transaction {
            val existing = queries.getApodByDate(remote.date).executeAsOneOrNull()
            val finalExplanation = remote.explanation ?: existing?.explanation
            val isFav = existing?.isFavorite ?: remote.isFavorite

            queries.insertApod(
                date = remote.date,
                explanation = finalExplanation,
                mediaType = remote.mediaType,
                serviceVersion = remote.serviceVersion,
                title = remote.title,
                urlHD = remote.urlHD,
                url = remote.url,
                thumbnailUrl = remote.thumbnailUrl,
                tags = remote.tags.toJsonString(),
                copyright = remote.copyright,
                isFavorite = isFav,
                createdAt = Clock.System.now().toString(),
                averageRating = remote.averageRating?.toLong(),
                totalVotes = remote.totalVotes?.toLong()
            )
        }
    }

    /**
     * Executes a fuzzy search.
     * We return the List<ApodResponse> directly to the UI.
     */
    suspend fun searchGlobal(query: String): List<ApodResponse> {
        return api.searchAllFields(query)
    }

    /**
     * Toggle favorite status in SQLDelight.
     * This is a local-only operation that makes the UI feel instant.
     */
    suspend fun toggleFavorite(date: String, isFavorite: Boolean) {
        // Fire and forget on a background thread
        withContext(Dispatchers.IO) {
            queries.updateFavorite(isFavorite = isFavorite, date)
        }
    }

    fun getLocalFavorites(): List<ApodEntity> {
        return queries.getAllFavorites().executeAsList()
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

fun ApodEntity.toResponse(): ApodResponse {
    return ApodResponse(
        date = this.date,
        title = this.title,
        explanation = this.explanation,
        url = this.url,
        urlHD = this.urlHD,
        mediaType = this.mediaType,
        copyright = this.copyright,
        thumbnailUrl = this.thumbnailUrl,
        // Convert the JSON string back into a List<String>
        tags = Json.decodeFromString<List<String>>(this.tags ?: "[]")
    )
}
