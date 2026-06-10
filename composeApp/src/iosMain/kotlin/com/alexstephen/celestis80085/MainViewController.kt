package com.alexstephen.celestis80085

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.alexstephen.celestis80085.ui.utils.LinkGenerator

private var deepLinkDate: String? = null

fun MainViewController() = ComposeUIViewController(
    configure = {
        // Enforce edge-to-edge by ignoring platform safe areas
        onFocusBehavior = OnFocusBehavior.DoNothing
    }
) {
    App(initialDeepLinkDate = deepLinkDate)
}

/**
 * Called from Swift to handle deep link URLs
 */
fun handleDeepLink(url: String) {
    deepLinkDate = LinkGenerator.extractDateFromLink(url)
}
