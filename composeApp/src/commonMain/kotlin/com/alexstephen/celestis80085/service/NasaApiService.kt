package com.alexstephen.celestis80085.service

import com.alexstephen.celestis80085.BuildKonfig
import com.alexstephen.celestis80085.model.ApodResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class DeviceTokenRequest(val token: String, val platform: String)

class NasaApi(private val client: HttpClient) {
    private val apiRoot = "${BuildKonfig.BASE_URL}/api"
    private val baseUrl = "$apiRoot/apod"

    suspend fun getApodFromServer(date: String? = null): ApodResponse {
    val response = client.get(baseUrl) {
     url { date?.let { parameters.append("date", it) } }
    }

    return when (response.status.value) {
     200 -> response.body()
     429 -> throw Exception("Rate limit reached. Please try again later.")
     404 -> throw Exception("Photo not found for this date.")
     else -> throw Exception("Unable to load photo. Please check your connection.")
    }
 }

 suspend fun getRandomApods(count: Int): List<ApodResponse> {
   val response = client.get("$baseUrl/random") {
    url { parameters.append("count", count.toString()) }
   }

   return when (response.status.value) {
    200 -> response.body<List<ApodResponse>>()
    // 204 No Content
    204 -> emptyList()
    400 -> throw Exception("Unable to fetch photos. Please try again.")
    else -> handleCommonErrors(response)
   }
 }

 suspend fun getApodRange(
     start: String,
     end: String,
     page: Int = 0,
     limit: Int = 30
 ): List<ApodResponse> {
     val response = client.get("$baseUrl/range") {
         url {
             parameters.append("start_date", start)
             parameters.append("end_date", end)
             parameters.append("page", page.toString())
             parameters.append("limit", limit.toString())
         }
     }

     return when (response.status.value) {
     200 -> response.body<List<ApodResponse>>()
             // 204 No Content
             204 -> emptyList()
             400 -> throw Exception("Invalid date range. Please select a valid range.")
             else -> handleCommonErrors(response)
         }
 }

 // The Fuzzy Text Search we built with Atlas Search
 suspend fun searchAllFields(query: String, page: Int): List<ApodResponse> {
   val response = client.get("$baseUrl/search") {
    url { parameters.append("q", query)
        parameters.append("page", page.toString())}
   }

   return when (response.status.value) {
    200 -> response.body<List<ApodResponse>>()
    403 -> throw Exception("Search unavailable. Please try again later.")
    504 -> throw Exception("Search is taking too long. Try a shorter search term.")
    else -> handleCommonErrors(response)
   }
 }

 private suspend fun handleCommonErrors(response: io.ktor.client.statement.HttpResponse): Nothing {
  when (response.status.value) {
   429 -> throw Exception("Rate limit reached. Please wait a moment.")
   500, 502, 503 -> throw Exception("Server is undergoing maintenance.")
   else -> throw Exception("Unable to connect. Please check your network and try again.")
  }
 }

 /**
  * Registers or refreshes the device push token with the Celestis backend.
  * The backend stores this token and uses it for the daily 05:15 UTC silent push.
  * Silent failures are acceptable — the FCM token is refreshed automatically by Firebase.
  */
 suspend fun registerDeviceToken(token: String, platform: String) {
     try {
         client.post("$apiRoot/devices/token") {
             contentType(ContentType.Application.Json)
             setBody(DeviceTokenRequest(token, platform))
         }
     } catch (_: Exception) {
         // Non-critical — token will be re-registered on the next app launch or token refresh
     }
 }
}