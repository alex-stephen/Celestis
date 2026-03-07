package com.example.astrolume.data

import com.example.astrolume.database.ApodEntity
import com.example.astrolume.database.AppDatabase
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.service.NasaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ApodRepository(
    private val api: NasaApi,
    private val database: AppDatabase
) {
    private val queries = database.appDatabaseQueries

    /**
     * Returns a Flow that emits the cached version immediately,
     * then fetches and updates from network.
     */
    fun observeLatestApod(): Flow<ApodEntity?> = flow {
        // 1. Get the latest one we have in the DB (highest date)
        val cached = queries.getLatestApod().executeAsOneOrNull()
        if (cached != null) emit(cached)

        // 2. Fetch from Network
        try {
            val remote = api.getApodFromServer(null)

            saveToLocal(remote)

            // 4. Emit the updated version
            emit(queries.getApodByDate(remote.date).executeAsOne())
        } catch (e: Exception) {
            // Network failed? No problem, the UI already has the 'cached' version.
        }
    }.flowOn(Dispatchers.Default)

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
        remotes.forEach { saveToLocal(it) }
        return remotes
    }

    /**
     * Fills a specific date range. Perfect for a "Calendar" view.
     */
    suspend fun fetchRange(start: String, end: String): List<ApodResponse> {
        val remotes = api.getApodRange(start, end)
        remotes.forEach { saveToLocal(it) }
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

    private fun saveToLocal(remote: ApodResponse) {
        val existing = queries.getApodByDate(remote.date).executeAsOneOrNull()

        // 2. If it exists, keep the user's favorite status. If not, default to false.
        val currentFavoriteStatus = existing?.isFavorite ?: false
        queries.insertApod(
            date = remote.date,
            explanation = remote.explanation,
            mediaType = remote.mediaType,
            serviceVersion = remote.serviceVersion,
            title = remote.title,
            urlHD = remote.urlHD,
            url = remote.url,
            thumbnailUrl = remote.thumbnailUrl,
            tags = remote.tags.toJsonString(),
            copyright = remote.copyright,
            isFavorite = currentFavoriteStatus,
            createdAt = Clock.System.now().toString(),
            averageRating = remote.averageRating?.toLong(),
            totalVotes = remote.totalVotes?.toLong()
        )
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
    fun toggleFavorite(date: String, shouldBeFavorite: Boolean) {
        queries.updateFavorite(isFavorite = shouldBeFavorite, date)
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
