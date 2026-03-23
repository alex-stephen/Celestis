package com.example.celestis.ui.utils

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a hardware back button.
    // Swift-side swipe gestures are typically handled by the UIViewController.
}