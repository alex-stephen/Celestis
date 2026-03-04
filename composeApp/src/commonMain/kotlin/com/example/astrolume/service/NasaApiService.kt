package com.example.astrolume.service

import com.example.astrolume.BuildKonfig
import com.example.astrolume.model.ApodResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class NasaApi(private val client: HttpClient) {
 //TODO: FIX THIS STUPID BUILDKONFIG
 private val baseUrl = BuildKonfig.BASE_URL

 suspend fun getApodFromServer(date: String? = null): ApodResponse {
  return client.get(baseUrl) {
   url {
    // This ensures parameters are appended correctly to the existing base
    date?.let { parameters.append("date", it) }
   }
  }.body()
 }

 suspend fun searchByTag(tag: String): List<ApodResponse> {
  // Check if the proxy actually uses /api/apod/search
  // or if it should be /api/search
  return client.get("$baseUrl/search") {
   url { parameters.append("tag", tag) }
  }.body()
 }
}