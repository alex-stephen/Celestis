package com.example.astrolume

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.astrolume.ui.ApodViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val viewModel: ApodViewModel = koinViewModel()

    // UI State
    val results by viewModel.results.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D17)) // Deep Space Black
                .padding(16.dp)
                .safeContentPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "AstroLume Engine Test",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Cyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status Bar
            Surface(
                color = if (status.contains("Error")) Color.Red.copy(alpha = 0.2f) else Color.DarkGray,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = if (isLoading) "⏳ Processing..." else "Status: $status",
                    modifier = Modifier.padding(8.dp),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Panel
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { viewModel.testSingle() }) { Text("Single (Today)") }
                Button(onClick = { viewModel.testRandom() }) { Text("Random (5)") }
                Button(onClick = { viewModel.testRange() }) { Text("Range (7 Days)") }
                Button(onClick = { viewModel.testSearch("Galaxy") }) { Text("Search 'Galaxy'") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results) { apod ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1E26))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(apod.title ?: "No Title", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(apod.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                            // Visual cue for Tags (Testing the Proxy Enrichment)
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                apod.tags.take(3).forEach { tag ->
                                    Text(
                                        "#$tag ",
                                        color = Color.Cyan,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}