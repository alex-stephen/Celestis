package com.example.celestis.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }

        install(HttpCache)

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000 // 15s
            connectTimeoutMillis = 10_000 // 10s
            socketTimeoutMillis = 15_000
        }

        // 3. Global Headers
        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            // Example: versioning your API calls for future-proofing
            header("Celestis-Version", "1.0.0")
        }

        // 4. Debugging (Only for non-production builds)
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY
        }

        expectSuccess = true
    }
}