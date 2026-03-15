package com.example.astrolume.ui.utils

import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import coil3.PlatformContext
import com.example.astrolume.model.ApodResponse

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

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share APOD")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(context, shareIntent, null)
}
