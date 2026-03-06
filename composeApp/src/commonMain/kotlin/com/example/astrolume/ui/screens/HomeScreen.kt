package com.example.astrolume.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.astrolume.ui.viewModels.HomeUiState

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (state) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }

            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Today's Feature",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    item {
                        // We will build a reusable ApodCard next!
                        Text(text = state.todayApod.title ?: "No Title")
                        Text(
                            text = state.todayApod.date,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    state.randomApod?.let { random ->
                        item {
                            Text(
                                text = "Random Discovery",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        item {
                            Text(text = random.title ?: "Unknown")
                        }
                    }
                }
            }
        }
    }
}