package com.lucid.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lucid.app.ui.screens.BriefingScreen
import com.lucid.app.ui.theme.LucidTheme

/**
 * Standalone Briefing Activity - for viewing communication synthesis
 */
class BriefingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LucidTheme(isLucidMode = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BriefingScreen(
                        onBackClick = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}
