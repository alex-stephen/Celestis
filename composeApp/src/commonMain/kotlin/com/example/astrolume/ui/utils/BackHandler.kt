package com.example.astrolume.ui.utils

import androidx.compose.runtime.Composable

@Composable
expect fun CommonBackHandler(enabled: Boolean = true, onBack: () -> Unit)