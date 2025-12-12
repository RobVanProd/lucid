package com.lucid.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.lucid.app.LucidApplication
import com.lucid.app.filter.IntentionAction
import com.lucid.app.filter.IntentionEngine
import com.lucid.app.ui.screens.BriefingScreen
import com.lucid.app.ui.screens.FocusedBrowseScreen
import com.lucid.app.ui.screens.ZeroInterfaceScreen
import com.lucid.app.ui.theme.LucidTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * LUCID Main Activity - The Cognitive Firewall Entry Point
 *
 * "We are not building a productivity app.
 * We are building a Cognitive Firewall."
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val preferences = LucidApplication.instance.preferences

            // State
            var isLucidMode by remember { mutableStateOf(true) }
            var currentScreen by remember { mutableStateOf<Screen>(Screen.ZeroInterface) }

            // Load preferences
            LaunchedEffect(Unit) {
                isLucidMode = preferences.isLucidMode.first()
            }

            LucidTheme(isLucidMode = isLucidMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            when {
                                targetState is Screen.ZeroInterface -> {
                                    // Dissolving back to zero interface
                                    fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(300))
                                }
                                else -> {
                                    // Emerging from intention
                                    fadeIn(animationSpec = tween(300)) + scaleIn(
                                        initialScale = 0.95f,
                                        animationSpec = tween(300)
                                    ) togetherWith fadeOut(animationSpec = tween(200))
                                }
                            }
                        },
                        label = "screenTransition"
                    ) { screen ->
                        when (screen) {
                            is Screen.ZeroInterface -> {
                                ZeroInterfaceScreen(
                                    isLucidMode = isLucidMode,
                                    onModeChange = { newMode ->
                                        isLucidMode = newMode
                                        lifecycleScope.launch {
                                            preferences.setLucidMode(newMode)
                                        }
                                    },
                                    onIntentionSubmit = { intention ->
                                        val action = IntentionEngine.analyze(intention)
                                        lifecycleScope.launch {
                                            preferences.setLastIntent(intention)
                                        }
                                        currentScreen = Screen.FocusedBrowse(action)
                                    },
                                    onBriefingClick = {
                                        currentScreen = Screen.Briefing
                                    }
                                )
                            }
                            is Screen.FocusedBrowse -> {
                                FocusedBrowseScreen(
                                    intentionAction = screen.action,
                                    isLucidMode = isLucidMode,
                                    onDissolve = {
                                        // The interface dissolves
                                        currentScreen = Screen.ZeroInterface
                                    }
                                )
                            }
                            is Screen.Briefing -> {
                                BriefingScreen(
                                    onBackClick = {
                                        currentScreen = Screen.ZeroInterface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Navigation screens
 */
sealed class Screen {
    data object ZeroInterface : Screen()
    data class FocusedBrowse(val action: IntentionAction) : Screen()
    data object Briefing : Screen()
}
