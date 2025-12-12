package com.lucid.app.ai

/**
 * LUCID AI Interface
 *
 * Provides intelligent intent classification, content summarization,
 * and entity extraction. Designed to work with on-device SLM (Qwen2.5-0.5B).
 */
interface LucidAI {

    /**
     * Check if the AI model is ready for inference
     */
    suspend fun isReady(): Boolean

    /**
     * Classify user intention into a category
     */
    suspend fun classifyIntent(text: String): IntentClassification

    /**
     * Summarize content to a specified length
     */
    suspend fun summarize(content: String, maxLength: Int = 200): String

    /**
     * Extract key points from content
     */
    suspend fun extractKeyPoints(content: String, count: Int = 5): List<String>

    /**
     * Generate a response based on context and query
     */
    suspend fun generateResponse(query: String, context: String): String
}

/**
 * Result of intent classification
 */
data class IntentClassification(
    val intentType: IntentType,
    val confidence: Float,
    val topic: String,
    val entities: Map<String, String> = emptyMap()
)

/**
 * Types of user intentions
 */
enum class IntentType {
    RECIPE,      // User wants a recipe
    DEFINITION,  // User wants to know what something is
    LEARN,       // User wants to learn about a topic
    SEARCH,      // General search
    NAVIGATE,    // Direct URL navigation
    CALCULATE,   // Math or conversion
    WEATHER,     // Weather information
    NEWS,        // News about a topic
    UNKNOWN      // Could not classify
}

/**
 * Model status for UI feedback
 */
sealed class ModelStatus {
    data object NotDownloaded : ModelStatus()
    data class Downloading(val progress: Float) : ModelStatus()
    data object Loading : ModelStatus()
    data object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}
