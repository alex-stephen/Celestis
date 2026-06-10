package com.alexstephen.celestis80085

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alexstephen.celestis80085.ui.utils.LinkGenerator

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Notification tap passes the date as an extra; URI deep links pass it via data URI.
        val deepLinkDate = intent?.getStringExtra(EXTRA_NOTIFICATION_DATE)
            ?: handleDeepLink(intent)

        setContent {
            App(initialDeepLinkDate = deepLinkDate)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val deepLinkDate = intent.getStringExtra(EXTRA_NOTIFICATION_DATE)
            ?: handleDeepLink(intent)

        if (deepLinkDate != null) {
            finish()
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_NOTIFICATION_DATE, deepLinkDate)
            })
        }
    }

    private fun handleDeepLink(intent: Intent?): String? =
        intent?.data?.toString()?.let { LinkGenerator.extractDateFromLink(it) }

    companion object {
        /** Extra key used by [ApodNotificationReceiver] to pass the APOD date on notification tap. */
        const val EXTRA_NOTIFICATION_DATE = "extra_notification_date"
    }
}
