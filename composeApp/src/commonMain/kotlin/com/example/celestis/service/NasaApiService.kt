package com.example.celestis.service

import com.example.celestis.BuildKonfig
import com.example.celestis.model.ApodResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class NasaApi(private val client: HttpClient) {
 private val baseUrl = BuildKonfig.BASE_URL

 suspend fun getApodFromServer(date: String? = null): ApodResponse {
    val response = client.get(baseUrl) {
     url { date?.let { parameters.append("date", it) } }
    }

    // Handle specific Proxy/NASA errors
    return when (response.status.value) {
     200 -> response.body()
     429 -> throw Exception("NASA Rate limit exceeded. Try again in an hour.")
     else -> throw Exception("Server Error: ${response.status.description}")
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
    400 -> throw Exception("Invalid count requested: $count")
    else -> handleCommonErrors(response)
   }
 }

 suspend fun getApodRange(start: String, end: String): List<ApodResponse> {
     val response = client.get("$baseUrl/range") {
         url {
             parameters.append("start_date", start)
             parameters.append("end_date", end)
         }
     }

     return when (response.status.value) {
     200 -> response.body<List<ApodResponse>>()
             // 204 No Content
             204 -> emptyList()
             400 -> throw Exception("Invalid range requested: $start - $end")
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
    // 403 Forbidden: Often occurs if Atlas Search API keys expire or rotate
    403 -> throw Exception("Search service temporarily unavailable.")
    // 504 Gateway Timeout: Atlas searches can be heavy
    504 -> throw Exception("Search timed out. Try a more specific term.")
    else -> handleCommonErrors(response)
   }
 }

 private suspend fun handleCommonErrors(response: io.ktor.client.statement.HttpResponse): Nothing {
  val errorBody = try { response.body<String>() } catch (e: Exception) { "Unknown Error" }
  when (response.status.value) {
   429 -> throw Exception("Rate limit reached. Please wait a moment.")
   500, 502, 503 -> throw Exception("Server is undergoing maintenance.")
   else -> throw Exception("Network Error: ${response.status.value} - $errorBody")
  }
 }

 suspend fun searchByTag(tag: String): List<ApodResponse> {
  // Check if the proxy actually uses /api/apod/search
  // or if it should be /api/search
  return client.get("$baseUrl/search") {
   url { parameters.append("tag", tag) }
  }.body()
 }
}