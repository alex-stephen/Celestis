package com.alexstephen.celestis80085.ui.utils

import coil3.PlatformContext
import com.alexstephen.celestis80085.model.ApodResponse
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

actual fun shareApod(apod: ApodResponse, context: PlatformContext) {
    val shareText = buildString {
        append("🌌 ${apod.title ?: "Astronomy Picture of the Day"}\n\n")
        append("📅 ${apod.date}\n\n")
        if (apod.explanation != null) {
            append("${apod.explanation}\n\n")
        }
        if (apod.url != null) {
            append("🔗 ${apod.url}")
        }
        if (apod.copyright != null) {
            append("\n\n© ${apod.copyright}")
        }
    }

    val activityViewController = UIActivityViewController(
        activityItems = listOf(shareText),
        applicationActivities = null
    )

    val window = UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val rootViewController = window?.rootViewController

    rootViewController?.presentViewController(
        viewControllerToPresent = activityViewController,
        animated = true,
        completion = null
    )
}
