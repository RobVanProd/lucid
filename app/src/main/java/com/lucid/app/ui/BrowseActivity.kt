package com.lucid.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lucid.app.filter.IntentionAction
import com.lucid.app.ui.screens.FocusedBrowseScreen
import com.lucid.app.ui.theme.LucidTheme

/**
 * Standalone Browse Activity - for launching focused browse sessions
 */
class BrowseActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TOPIC = "extra_topic"
        const val EXTRA_LUCID_MODE = "extra_lucid_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url = intent.getStringExtra(EXTRA_URL) ?: "https://duckduckgo.com"
        val topic = intent.getStringExtra(EXTRA_TOPIC) ?: ""
        val isLucidMode = intent.getBooleanExtra(EXTRA_LUCID_MODE, true)

        setContent {
            LucidTheme(isLucidMode = isLucidMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FocusedBrowseScreen(
                        intentionAction = IntentionAction.Navigate(url).let {
                            if (topic.isNotEmpty()) {
                                IntentionAction.Search(topic, url)
                            } else {
                                it
                            }
                        },
                        isLucidMode = isLucidMode,
                        onDissolve = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}
