package com.example.celestis.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class AndroidAppActionManager(private val context: Context) : AppActionManager {
    override fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun reportBug() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "Celestis bug report")
            putExtra(
                Intent.EXTRA_TEXT,
                "Describe what happened:\n\nSteps to reproduce:\n\nDevice and Android version:\n"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Report a bug").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun leaveReview() {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching {
            context.startActivity(marketIntent)
        }.onFailure {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    override fun shareApp() {
        val appUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Celestis - daily space images from NASA: $appUrl")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Celestis").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private companion object {
        const val SUPPORT_EMAIL = "support.celestis@gmail.com"
    }
}
