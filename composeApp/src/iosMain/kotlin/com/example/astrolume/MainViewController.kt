package com.example.astrolume

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController(
    configure = {
        // Enforce edge-to-edge by ignoring platform safe areas
        onFocusBehavior = OnFocusBehavior.DoNothing
    }
) {
    App()
}