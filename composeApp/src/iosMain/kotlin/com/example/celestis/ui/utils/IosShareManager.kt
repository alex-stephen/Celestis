package com.example.celestis.ui.utils

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IosShareManager : ShareManager {
    override fun shareData(title: String, text: String) {
        val items = listOf(text)
        val activityViewController = UIActivityViewController(items, null)

        // Find the top-most view controller to present the share sheet
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController

        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )
    }
}