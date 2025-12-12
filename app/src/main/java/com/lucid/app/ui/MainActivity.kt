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
import com.lucid.app.ai.LucidAIImpl
import com.lucid.app.content.*
import com.lucid.app.filter.IntentionAction
import com.lucid.app.filter.IntentionEngine
import com.lucid.app.ui.screens.*
import com.lucid.app.ui.theme.LucidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LUCID Main Activity - The Cognitive Firewall Entry Point
 *
 * "We are not building a productivity app.
 * We are building a Cognitive Firewall."
 *
 * v0.2 - Now with intelligent content extraction and native display
 */
class MainActivity : ComponentActivity() {

    private lateinit var ai: LucidAIImpl
    private lateinit var contentRepository: ContentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AI and content systems
        ai = LucidAIImpl(this)
        contentRepository = ContentRepository(ai)
        IntentionEngine.initialize(ai)

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
                                    fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(300))
                                }
                                else -> {
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
                                        lifecycleScope.launch {
                                            preferences.setLastIntent(intention)
                                            // Use AI-powered analysis
                                            val action = withContext(Dispatchers.Default) {
                                                IntentionEngine.analyzeWithAI(intention)
                                            }
                                            currentScreen = Screen.ContentLoading(action)
                                        }
                                    },
                                    onBriefingClick = {
                                        currentScreen = Screen.Briefing
                                    }
                                )
                            }
                            is Screen.ContentLoading -> {
                                ContentLoadingScreen(
                                    action = screen.action,
                                    contentRepository = contentRepository,
                                    onContentLoaded = { loadedScreen ->
                                        currentScreen = loadedScreen
                                    },
                                    onError = { message ->
                                        currentScreen = Screen.Error(message, screen.action)
                                    },
                                    onBack = {
                                        currentScreen = Screen.ZeroInterface
                                    }
                                )
                            }
                            is Screen.RecipeContent -> {
                                RecipeScreen(
                                    recipe = screen.recipe,
                                    onBack = { currentScreen = Screen.ZeroInterface }
                                )
                            }
                            is Screen.InfoContent -> {
                                InfoScreen(
                                    definition = screen.definition,
                                    onBack = { currentScreen = Screen.ZeroInterface }
                                )
                            }
                            is Screen.LearnContent -> {
                                LearnScreen(
                                    content = screen.content,
                                    onBack = { currentScreen = Screen.ZeroInterface }
                                )
                            }
                            is Screen.GeneralContent -> {
                                ContentScreen(
                                    content = screen.content,
                                    onBack = { currentScreen = Screen.ZeroInterface }
                                )
                            }
                            is Screen.FocusedBrowse -> {
                                // Legacy WebView screen for direct URL navigation
                                FocusedBrowseScreen(
                                    intentionAction = screen.action,
                                    isLucidMode = isLucidMode,
                                    onDissolve = { currentScreen = Screen.ZeroInterface }
                                )
                            }
                            is Screen.Briefing -> {
                                BriefingScreen(
                                    onBackClick = { currentScreen = Screen.ZeroInterface }
                                )
                            }
                            is Screen.Error -> {
                                ErrorScreen(
                                    message = screen.message,
                                    onRetry = {
                                        currentScreen = Screen.ContentLoading(screen.action)
                                    },
                                    onBack = { currentScreen = Screen.ZeroInterface }
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
 * Content Loading Screen - Fetches content and transitions to appropriate screen
 */
@Composable
private fun ContentLoadingScreen(
    action: IntentionAction,
    contentRepository: ContentRepository,
    onContentLoaded: (Screen) -> Unit,
    onError: (String) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(action) {
        isLoading = true

        // For direct URL navigation, use WebView
        if (action is IntentionAction.Navigate) {
            onContentLoaded(Screen.FocusedBrowse(action))
            return@LaunchedEffect
        }

        // Fetch content based on action type
        val result = when (action) {
            is IntentionAction.Recipe -> contentRepository.getRecipe(action.topic)
            is IntentionAction.Define -> contentRepository.getDefinition(action.topic)
            is IntentionAction.Learn -> contentRepository.getLearnContent(action.topic)
            else -> contentRepository.searchContent(action.topic)
        }

        when (result) {
            is ContentResult.Success -> {
                val screen = when (val data = result.data) {
                    is Recipe -> Screen.RecipeContent(data)
                    is Definition -> Screen.InfoContent(data)
                    is LearnContent -> Screen.LearnContent(data)
                    is ExtractedContent -> Screen.GeneralContent(data)
                    else -> Screen.GeneralContent(
                        ExtractedContent(
                            title = action.topic,
                            summary = data.toString(),
                            content = data.toString()
                        )
                    )
                }
                onContentLoaded(screen)
            }
            is ContentResult.Error -> {
                onError(result.message)
            }
            is ContentResult.Loading -> {
                // Still loading
            }
        }

        isLoading = false
    }

    if (isLoading) {
        LoadingScreen(
            message = when (action) {
                is IntentionAction.Recipe -> "Finding recipe..."
                is IntentionAction.Define -> "Looking up definition..."
                is IntentionAction.Learn -> "Gathering information..."
                else -> "Searching..."
            }
        )
    }
}

/**
 * Navigation screens
 */
sealed class Screen {
    data object ZeroInterface : Screen()
    data class ContentLoading(val action: IntentionAction) : Screen()
    data class RecipeContent(val recipe: Recipe) : Screen()
    data class InfoContent(val definition: Definition) : Screen()
    data class LearnContent(val content: com.lucid.app.content.LearnContent) : Screen()
    data class GeneralContent(val content: ExtractedContent) : Screen()
    data class FocusedBrowse(val action: IntentionAction) : Screen()
    data object Briefing : Screen()
    data class Error(val message: String, val action: IntentionAction) : Screen()
}
