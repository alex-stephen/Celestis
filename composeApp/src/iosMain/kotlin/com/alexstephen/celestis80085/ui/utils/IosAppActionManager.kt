package com.alexstephen.celestis80085.ui.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIActivityViewController

class IosAppActionManager : AppActionManager {
    override fun openNotificationSettings() {
        openUrl(UIApplicationOpenSettingsURLString)
    }

    override fun reportBug() {
        val subject = "Celestis%20bug%20report"
        val body = "Describe%20what%20happened:%0A%0ASteps%20to%20reproduce:%0A%0ADevice%20and%20iOS%20version:%0A"
        openUrl("mailto:$SUPPORT_EMAIL?subject=$subject&body=$body")
    }

    override fun leaveReview() {
        openUrl("https://apps.apple.com/search?term=Celestis")
    }

    override fun shareApp() {
        val text = "Celestis - daily space images from NASA: https://apps.apple.com/app/celestis"
        val window = UIApplication.sharedApplication.keyWindow
        val viewController = window?.rootViewController ?: return
        val activityViewController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )
        viewController.presentViewController(activityViewController, animated = true, completion = null)
    }

    private fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }

    private companion object {
        const val SUPPORT_EMAIL = "support.celestis@gmail.com"
    }
}
