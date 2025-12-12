package com.lucid.app.filter

import com.lucid.app.ai.IntentClassification
import com.lucid.app.ai.IntentType
import com.lucid.app.ai.LucidAI
import java.net.URLEncoder

/**
 * The Intention Engine - Translates human intention into focused action
 *
 * "If you type 'Learn about Renaissance Architecture,' LUCID spins up
 * a temporary, ephemeral interface dedicated solely to that."
 *
 * Now enhanced with AI-powered intent classification.
 */
object IntentionEngine {

    private var ai: LucidAI? = null

    /**
     * Initialize with AI model
     */
    fun initialize(lucidAI: LucidAI) {
        ai = lucidAI
    }

    /**
     * Analyze the intention using AI and determine the best action
     */
    suspend fun analyzeWithAI(intention: String): IntentionAction {
        val aiInstance = ai ?: return analyze(intention)

        return try {
            val classification = aiInstance.classifyIntent(intention)
            classificationToAction(classification)
        } catch (e: Exception) {
            // Fallback to rule-based analysis
            analyze(intention)
        }
    }

    /**
     * Convert AI classification to IntentionAction
     */
    private fun classificationToAction(classification: IntentClassification): IntentionAction {
        val topic = classification.topic

        return when (classification.intentType) {
            IntentType.RECIPE -> IntentionAction.Recipe(topic, buildRecipeUrl(topic))
            IntentType.DEFINITION -> IntentionAction.Define(topic, buildDefineUrl(topic))
            IntentType.LEARN -> IntentionAction.Learn(topic, buildLearnUrl(topic))
            IntentType.SEARCH -> IntentionAction.Search(topic, buildSearchUrl(topic))
            IntentType.NAVIGATE -> IntentionAction.Navigate(topic)
            IntentType.CALCULATE -> IntentionAction.Calculate(topic)
            IntentType.WEATHER -> IntentionAction.Weather(topic)
            IntentType.NEWS -> IntentionAction.News(topic, buildSearchUrl("$topic news"))
            IntentType.UNKNOWN -> IntentionAction.Search(topic, buildSearchUrl(topic))
        }
    }

    /**
     * Rule-based analysis (fallback)
     */
    fun analyze(intention: String): IntentionAction {
        val normalized = intention.trim().lowercase()

        return when {
            // Direct URL
            isUrl(intention) -> IntentionAction.Navigate(intention)

            // Search intents
            normalized.startsWith("search ") ||
            normalized.startsWith("find ") ||
            normalized.startsWith("look up ") -> {
                val query = intention.substringAfter(" ").trim()
                IntentionAction.Search(query, buildSearchUrl(query))
            }

            // Learning intents
            normalized.startsWith("learn ") ||
            normalized.startsWith("understand ") ||
            normalized.startsWith("study ") ||
            normalized.startsWith("research ") ||
            normalized.startsWith("tell me about ") -> {
                val topic = intention.substringAfter(" ").trim()
                IntentionAction.Learn(topic, buildLearnUrl(topic))
            }

            // Reading intents
            normalized.startsWith("read ") ||
            normalized.startsWith("article ") ||
            normalized.startsWith("news ") -> {
                val topic = intention.substringAfter(" ").trim()
                IntentionAction.Read(topic, buildSearchUrl(topic))
            }

            // Recipe intents
            normalized.contains("recipe") ||
            normalized.startsWith("cook ") ||
            normalized.startsWith("make ") ||
            normalized.startsWith("how to cook ") ||
            normalized.startsWith("how to make ") -> {
                val dish = extractRecipeTopic(intention)
                IntentionAction.Recipe(dish, buildRecipeUrl(dish))
            }

            // Definition intents
            normalized.startsWith("what is ") ||
            normalized.startsWith("what are ") ||
            normalized.startsWith("define ") ||
            normalized.startsWith("meaning of ") ||
            normalized.startsWith("who is ") ||
            normalized.startsWith("who was ") -> {
                val term = extractDefinitionTopic(intention)
                IntentionAction.Define(term, buildDefineUrl(term))
            }

            // Default: treat as search
            else -> {
                IntentionAction.Search(intention, buildSearchUrl(intention))
            }
        }
    }

    private fun isUrl(text: String): Boolean {
        return text.startsWith("http://") ||
               text.startsWith("https://") ||
               text.startsWith("www.") ||
               text.matches(Regex("^[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}.*"))
    }

    private fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        // Use DuckDuckGo for privacy
        return "https://duckduckgo.com/?q=$encoded"
    }

    private fun buildLearnUrl(topic: String): String {
        val encoded = URLEncoder.encode(topic, "UTF-8")
        // Wikipedia for learning
        return "https://en.wikipedia.org/wiki/Special:Search?search=$encoded"
    }

    private fun buildRecipeUrl(dish: String): String {
        val encoded = URLEncoder.encode("$dish recipe", "UTF-8")
        return "https://duckduckgo.com/?q=$encoded"
    }

    private fun buildDefineUrl(term: String): String {
        val encoded = URLEncoder.encode(term, "UTF-8")
        return "https://en.wiktionary.org/wiki/Special:Search?search=$encoded"
    }

    private fun extractRecipeTopic(intention: String): String {
        return intention
            .lowercase()
            .replace("recipe for", "")
            .replace("recipe", "")
            .replace("how to cook", "")
            .replace("how to make", "")
            .replace("cook", "")
            .replace("make", "")
            .trim()
    }

    private fun extractDefinitionTopic(intention: String): String {
        return intention
            .lowercase()
            .replace("what is", "")
            .replace("define", "")
            .replace("meaning of", "")
            .replace("the", "")
            .replace("a ", "")
            .replace("an ", "")
            .trim()
    }
}

/**
 * The result of intention analysis
 */
sealed class IntentionAction {
    abstract val topic: String
    abstract val url: String

    data class Navigate(override val url: String) : IntentionAction() {
        override val topic: String = url
    }

    data class Search(override val topic: String, override val url: String) : IntentionAction()

    data class Learn(override val topic: String, override val url: String) : IntentionAction()

    data class Read(override val topic: String, override val url: String) : IntentionAction()

    data class Recipe(override val topic: String, override val url: String) : IntentionAction()

    data class Define(override val topic: String, override val url: String) : IntentionAction()

    data class Calculate(override val topic: String) : IntentionAction() {
        override val url: String = ""
    }

    data class Weather(override val topic: String) : IntentionAction() {
        override val url: String = ""
    }

    data class News(override val topic: String, override val url: String) : IntentionAction()
}
