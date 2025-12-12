package com.lucid.app.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * LUCID AI Implementation
 *
 * Uses a hybrid approach:
 * 1. First tries native llama.cpp inference if available
 * 2. Falls back to enhanced rule-based classification
 *
 * The native inference can be enabled by:
 * - Building llama.cpp for Android
 * - Downloading a GGUF model
 * - Calling LlamaInference.loadModel()
 */
class LucidAIImpl(private val context: Context) : LucidAI {

    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.Ready)
    val status: StateFlow<ModelStatus> = _status

    // Native inference engine (lazy initialization)
    private val llamaInference: LlamaInference by lazy {
        LlamaInference.getInstance(context)
    }

    // Keywords for intent classification
    private val recipeKeywords = listOf(
        "recipe", "cook", "bake", "make", "prepare", "how to make",
        "ingredients for", "dish", "meal", "food", "cuisine"
    )

    private val definitionKeywords = listOf(
        "what is", "what are", "define", "definition", "meaning of",
        "explain", "what does", "who is", "who was"
    )

    private val learnKeywords = listOf(
        "learn", "understand", "study", "research", "tell me about",
        "how does", "why does", "history of", "guide to", "tutorial"
    )

    private val calculateKeywords = listOf(
        "calculate", "convert", "how much", "how many", "math",
        "equals", "+", "-", "*", "/", "percent"
    )

    private val weatherKeywords = listOf(
        "weather", "temperature", "forecast", "rain", "sunny", "cloudy"
    )

    private val newsKeywords = listOf(
        "news", "latest", "recent", "today", "headlines", "update on"
    )

    /**
     * Check if native inference is available
     */
    fun isNativeInferenceAvailable(): Boolean = llamaInference.isNativeAvailable()

    override suspend fun isReady(): Boolean = true

    override suspend fun classifyIntent(text: String): IntentClassification {
        return withContext(Dispatchers.Default) {
            // Try native inference first if available
            val nativeResult = llamaInference.classifyIntent(text)
            if (nativeResult != null) {
                return@withContext nativeResult
            }

            // Fall back to rule-based classification
            classifyIntentRuleBased(text)
        }
    }

    /**
     * Rule-based intent classification (fallback when native inference unavailable)
     */
    private fun classifyIntentRuleBased(text: String): IntentClassification {
        val normalized = text.trim().lowercase()

        // Check for direct URL
        if (normalized.startsWith("http://") || normalized.startsWith("https://") ||
            normalized.contains(".com") || normalized.contains(".org")) {
            return IntentClassification(
                intentType = IntentType.NAVIGATE,
                confidence = 0.95f,
                topic = text.trim()
            )
        }

        // Recipe detection
        if (recipeKeywords.any { normalized.contains(it) }) {
            val topic = extractTopic(normalized, recipeKeywords)
            return IntentClassification(
                intentType = IntentType.RECIPE,
                confidence = 0.9f,
                topic = topic
            )
        }

        // Definition detection
        if (definitionKeywords.any { normalized.startsWith(it) || normalized.contains(it) }) {
            val topic = extractTopic(normalized, definitionKeywords)
            return IntentClassification(
                intentType = IntentType.DEFINITION,
                confidence = 0.9f,
                topic = topic
            )
        }

        // Learning detection
        if (learnKeywords.any { normalized.startsWith(it) || normalized.contains(it) }) {
            val topic = extractTopic(normalized, learnKeywords)
            return IntentClassification(
                intentType = IntentType.LEARN,
                confidence = 0.85f,
                topic = topic
            )
        }

        // Calculate detection
        if (calculateKeywords.any { normalized.contains(it) }) {
            return IntentClassification(
                intentType = IntentType.CALCULATE,
                confidence = 0.85f,
                topic = normalized
            )
        }

        // Weather detection
        if (weatherKeywords.any { normalized.contains(it) }) {
            val topic = extractTopic(normalized, weatherKeywords)
            return IntentClassification(
                intentType = IntentType.WEATHER,
                confidence = 0.85f,
                topic = topic
            )
        }

        // News detection
        if (newsKeywords.any { normalized.contains(it) }) {
            val topic = extractTopic(normalized, newsKeywords)
            return IntentClassification(
                intentType = IntentType.NEWS,
                confidence = 0.8f,
                topic = topic
            )
        }

        // Default to search with the full query as topic
        return IntentClassification(
            intentType = IntentType.SEARCH,
            confidence = 0.6f,
            topic = text.trim()
        )
    }

    override suspend fun summarize(content: String, maxLength: Int): String {
        return withContext(Dispatchers.Default) {
            // Simple extractive summarization - take first sentences
            val sentences = content.split(Regex("[.!?]"))
                .map { it.trim() }
                .filter { it.length > 20 }

            val summary = StringBuilder()
            for (sentence in sentences) {
                if (summary.length + sentence.length > maxLength) break
                if (summary.isNotEmpty()) summary.append(". ")
                summary.append(sentence)
            }

            if (summary.isEmpty() && content.isNotEmpty()) {
                content.take(maxLength)
            } else {
                summary.toString()
            }
        }
    }

    override suspend fun extractKeyPoints(content: String, count: Int): List<String> {
        return withContext(Dispatchers.Default) {
            // Extract sentences that likely contain key information
            val sentences = content.split(Regex("[.!?]"))
                .map { it.trim() }
                .filter { it.length in 30..200 }

            // Simple heuristic: sentences with numbers, definitions, or key phrases
            val scored = sentences.map { sentence ->
                var score = 0
                if (sentence.contains(Regex("\\d+"))) score += 2  // Has numbers
                if (sentence.contains(" is ") || sentence.contains(" are ")) score += 3  // Definition-like
                if (sentence.contains("important") || sentence.contains("key") ||
                    sentence.contains("main") || sentence.contains("primary")) score += 2
                if (sentence.length > 50) score += 1  // Substantial content
                sentence to score
            }

            scored.sortedByDescending { it.second }
                .take(count)
                .map { it.first }
        }
    }

    override suspend fun generateResponse(query: String, context: String): String {
        // For now, return summarized context
        // This will be replaced with actual LLM generation
        return summarize(context, 300)
    }

    private fun extractTopic(text: String, keywords: List<String>): String {
        var topic = text
        for (keyword in keywords) {
            topic = topic.replace(keyword, "").trim()
        }
        // Clean up common words
        topic = topic.replace(Regex("^(a|an|the|about|for|to)\\s+"), "")
        return topic.trim().ifEmpty { text }
    }
}
