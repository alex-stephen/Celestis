package com.example.astrolume.service

import com.example.astrolume.BuildKonfig
import com.example.astrolume.model.ApodResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class NasaApi(private val client: HttpClient) {
 private val baseUrl = BuildKonfig.BASE_URL

 suspend fun getApodFromServer(date: String? = null): ApodResponse {
  // Updated to use query params: /api/apod?date=YYYY-MM-DD
  return client.get(baseUrl) {
   url { date?.let { parameters.append("date", it) } }
  }.body()
 }

 suspend fun searchByTag(tag: String): List<ApodResponse> {
  // Matches your new backend endpoint: /api/apod/search?tag=Mars
  return client.get("$baseUrl/search") {
   url { parameters.append("tag", tag) }
  }.body()
 }
}