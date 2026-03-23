package com.example.celestis.ui.utils

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
}