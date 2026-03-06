package com.example.astrolume.service

import com.example.astrolume.BuildKonfig
import com.example.astrolume.model.ApodResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class NasaApi(private val client: HttpClient) {
 private val baseUrl = BuildKonfig.BASE_URL

 suspend fun getApodFromServer(date: String? = null): ApodResponse {
  return client.get(baseUrl) {
   url {
    // This ensures parameters are appended correctly to the existing base
    date?.let { parameters.append("date", it) }
   }
  }.body()
 }

 suspend fun getRandomApods(count: Int): List<ApodResponse> {
  return client.get("$baseUrl/random") {
   url { parameters.append("count", count.toString()) }
  }.body()
 }

 suspend fun getApodRange(start: String, end: String): List<ApodResponse> {
  return client.get("$baseUrl/range") {
   url {
    parameters.append("start_date", start)
    parameters.append("end_date", end)
   }
  }.body()
 }

 // The Fuzzy Text Search we built with Atlas Search
 suspend fun searchAllFields(query: String): List<ApodResponse> {
  return client.get("$baseUrl/search") {
   url { parameters.append("q", query) }
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