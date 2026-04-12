package com.example.celestis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.celestis.ui.utils.LinkGenerator

class MainActivity : ComponentActivity() {

    /**
     * Requests POST_NOTIFICATIONS permission on Android 13+ (API 33).
     * [CelestisFcmService.onNewToken] whenever Firebase issues or rotates a token.
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result is handled passively — FCM takes care of the rest */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        /** Extra key used by [ApodNotificationReceiver] to pass the APOD date on notification tap. */
        const val EXTRA_NOTIFICATION_DATE = "extra_notification_date"
    }
}