package com.example.celestis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.celestis.ui.utils.LinkGenerator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val deepLinkDate = handleDeepLink(intent)
        
        setContent {
            App(initialDeepLinkDate = deepLinkDate)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Handle deep link when app is already running
        val deepLinkDate = handleDeepLink(intent)
        if (deepLinkDate != null) {
            // Restart the activity with the new deep link
            finish()
            startActivity(Intent(this, MainActivity::class.java).apply {
                data = intent.data
            })
        }
    }
    
    private fun handleDeepLink(intent: Intent?): String? {
        return intent?.data?.toString()?.let { url ->
            LinkGenerator.extractDateFromLink(url)
        }
    }
}
