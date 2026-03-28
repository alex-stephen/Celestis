package com.example.celestis.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.celestis.Platform
import com.example.celestis.database.ApodEntity
import com.example.celestis.database.AppDatabase
import com.example.celestis.model.ApodResponse
import com.example.celestis.service.NasaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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


    /**
     * Returns a Flow that emits the cached version immediately,
     * then fetches and updates from network.
     */
    fun observeLatestApod(): Flow<ApodResponse?> {
        return queries.getLatestApod()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { entity ->
                entity?.toResponse() // Map the DB Entity back to your Response model
            }
    }

    suspend fun refreshLatest() = withContext(Dispatchers.IO) {
        try {
            val remote = api.getApodFromServer(null)
            // PRE-CACHE: Start fetching the image as soon as we have metadata
            precacheImage(remote.url)
            saveToLocal(remote)
        } catch (e: Exception) {
            // Log to Sentry/Crashlytics, but don't crash the Flow
        }
    }

    suspend fun fetchApod(date: String? = null): ApodEntity = withContext(Dispatchers.IO) {
        // 1. Local check
        if (date != null) {
            val cached = queries.getApodByDate(date).executeAsOneOrNull()
            if (cached != null) return@withContext cached
        }

        // 2. Network fetch
        val remote = api.getApodFromServer(date)

        // 3. Persist to SQL
        saveToLocal(remote)

        queries.getApodByDate(remote.date).executeAsOne()
    }

    /**
     * Gets random APODs. We save them locally as we get them to
     * populate the user's "Discovery" cache.
     */
    suspend fun fetchRandom(count: Int): List<ApodResponse> = withContext(Dispatchers.IO) {
        val remotes = api.getRandomApods(count)
        database.transaction {
            remotes.forEach { innerSaveToLocal(it) }
        }
        // Cache images after DB is confirmed
        remotes.forEach { precacheImage(it.url) }
        return@withContext remotes
    }

    /**
     * Fills a specific date range with pagination support.
     * Perfect for a "Calendar" view with infinite scroll.
     */
    suspend fun fetchRange(
        start: String,
        end: String,
        page: Int = 0,
        limit: Int = 30
    ): List<ApodResponse> = withContext(Dispatchers.IO) {
        val remotes = api.getApodRange(start, end, page, limit)

        database.transaction {
            remotes.forEach { apod ->
                innerSaveToLocal(apod)
            }
        }
        return@withContext remotes
    }

    /**
     * Simple cross-platform search with pagination.
     * Searches both local database and remote API.
     */
    suspend fun searchWithPagination(query: String, page: Int): List<ApodResponse> = withContext(Dispatchers.IO) {
        val pageSize = 20L
        val offset = page * pageSize
        
        // First, try to get results from local cache
        val searchPattern = "%$query%"
        val localResults = queries.searchLocalApods(
            query = searchPattern,
            limit = pageSize,
            offset = offset
        ).executeAsList()
        
        // If we have enough local results, return them
        if (localResults.size >= pageSize || page > 0) {
            return@withContext localResults.map { it.toResponse() }
        }
        
        // Otherwise, fetch from remote
        try {
            val remoteResults = api.searchAllFields(query, page)
            
            // Save to database for future searches
            database.transaction {
                remoteResults.forEach { apod ->
                    innerSaveToLocal(apod)
                }
            }
            
            return@withContext remoteResults
        } catch (e: Exception) {
            // If remote fails, return whatever local results we have
            return@withContext localResults.map { it.toResponse() }
        }
    }
    private fun precacheImage(url: String?) {
        url ?: return
        val request = ImageRequest.Builder(platform.context) // platformContext provided via KMP
            .data(url)
            .crossfade(true)
            .build()
        imageLoader.enqueue(request)
    }

    fun innerSaveToLocal(remote: ApodResponse, forceFavorite: Boolean? = null) {
        val existing = queries.getApodByDate(remote.date).executeAsOneOrNull()
        val finalExplanation = remote.explanation ?: existing?.explanation
        val isFav = forceFavorite ?: existing?.isFavorite ?: remote.isFavorite

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
            createdAt = Clock.System.now().toEpochMilliseconds().toString(),
            averageRating = remote.averageRating?.toLong(),
            totalVotes = remote.totalVotes?.toLong()
        )
    }

    /**
     * Public entry point for saving a single APOD.
     */
    private fun saveToLocal(remote: ApodResponse) {
        database.transaction {
            innerSaveToLocal(remote)
        }
    }

    /**
     * Toggle favorite status in SQLDelight.
     * This is a local-only operation that makes the UI feel instant.
     */
    suspend fun toggleFavorite(date: String, isFavorite: Boolean, apod: ApodResponse? = null) = withContext(Dispatchers.IO) {
        database.transaction {
            // 1. Check if it exists
            val existing = queries.getApodByDate(date).executeAsOneOrNull()

            if (existing == null && apod != null) {
                // 2. If it's a random image not yet saved, INSERT it now
                innerSaveToLocal(apod, forceFavorite = isFavorite)
            } else {
                // 3. If it already exists, just flip the bit
                queries.updateFavorite(isFavorite, date)
            }
        }
    }

    fun getLocalFavorites(): Flow<List<ApodEntity>> {
        return queries.getAllFavorites()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    /**
     * Observes all cached APODs from the local database.
     * Used for offline mode to display all available content.
     */
    fun observeAllCachedApods(): Flow<List<ApodEntity>> {
        return queries.getAllApods()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    /**
     * Gets random APODs from the local cache.
     * Used for offline mode random button functionality.
     */
    suspend fun getRandomCachedApods(count: Int): List<ApodEntity> = withContext(Dispatchers.Default) {
        queries.getRandomCachedApods(count.toLong()).executeAsList()
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
    suspend fun pruneCacheIfNeeded() = withContext(Dispatchers.IO) {
        val nonFavCount = queries.countNonFavorites().executeAsOne()

        // Only prune if the database is getting "heavy" (e.g., > 200 cached randoms)
        if (nonFavCount > 500) {
            try {
                queries.deleteOldNonFavorites()
            } catch (e: Exception) {
                // Log "Maintenance Failed" but don't crash the user's experience
            }
        }
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
        isFavorite = this.isFavorite,
        tags = try {
            Json.decodeFromString<List<String>>(this.tags ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }
    )
}

fun ApodResponse.toEntityArgs(isFavOverride: Boolean? = null): ApodEntity {
    return ApodEntity(
        date = this.date,
        explanation = this.explanation,
        mediaType = this.mediaType,
        serviceVersion = this.serviceVersion,
        title = this.title,
        urlHD = this.urlHD,
        url = this.url,
        thumbnailUrl = this.thumbnailUrl,
        tags = Json.encodeToString(this.tags),
        copyright = this.copyright,
        isFavorite = isFavOverride ?: this.isFavorite,
        createdAt = Clock.System.now().toEpochMilliseconds().toString(),
        averageRating = this.averageRating?.toLong(),
        totalVotes = this.totalVotes?.toLong()
    )
}