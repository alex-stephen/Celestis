package com.example.astrolume

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.astrolume.data.ApodRepository
import com.example.astrolume.database.ApodEntity
import com.example.astrolume.database.AppDatabase
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.service.NasaApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun App(database: AppDatabase) {
    // 1. Setup (In a real app, use a ViewModel, but this is perfect for testing)
    val client = remember {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            // Add this to force the app to recognize JSON even if the server is messy
            expectSuccess = true
        }
    }
    val api = remember { NasaApi(client) }
    val repository = remember { ApodRepository(api, database) }

    // 2. UI State
    var results by remember { mutableStateOf<List<ApodResponse>>(emptyList()) }
    var singleApod by remember { mutableStateOf<ApodEntity?>(null) }
    var statusMessage by remember { mutableStateOf("Ready") }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().safeContentPadding().background(Color(0xFF121212)), // Dark space theme
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AstroLume Proxy Diagnostic", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("Status: $statusMessage", color = Color.Cyan)

            Row(modifier = Modifier.padding(8.dp)) {
                // Test 1: Fetch Single (Persists to SQL)
                Button(onClick = {
                    statusMessage = "Fetching 2024-03-01..."
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            singleApod = repository.fetchApod("2024-03-01")
                            statusMessage = "Success: Saved ${singleApod?.title} to SQL"
                        } catch (e: Exception) {
                            statusMessage = "Error: ${e.message}"
                        }
                    }
                }) { Text("Test Single") }

                Spacer(Modifier.width(8.dp))

                // Test 2: Search by Tag (From Proxy Server)
                Button(onClick = {
                    statusMessage = "Searching for 'Mars'..."
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            results = repository.searchByTag("Mars")
                            statusMessage = "Found ${results.size} Mars images"
                        } catch (e: Exception) {
                            statusMessage = "Search Error: ${e.message}"
                        }
                    }
                }) { Text("Test Search") }
            }

            Divider(color = Color.Gray)

            // Results List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (singleApod != null) {
                        Text("LAST CACHED ITEM:", color = Color.Yellow)
                        Text("Title: ${singleApod!!.title}", color = Color.White)
                        Text("Tags stored in SQL: ${singleApod!!.tags}", color = Color.Gray)
                        Spacer(Modifier.height(20.dp))
                    }
                }

                items(results) { item ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.title ?: "No Title", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(item.date, color = Color.LightGray)
                        Text("Tags: ${item.tags.joinToString()}", color = Color.Cyan, style = MaterialTheme.typography.bodySmall)
                        Divider(modifier = Modifier.padding(top = 8.dp), color = Color(0xFF333333))
                    }
                }
            }
        }
    }
}