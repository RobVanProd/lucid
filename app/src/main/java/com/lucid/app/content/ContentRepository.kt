package com.lucid.app.content

import com.lucid.app.ai.IntentClassification
import com.lucid.app.ai.IntentType
import com.lucid.app.ai.LucidAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Content Repository
 *
 * Orchestrates content fetching, parsing, and AI summarization.
 * This is the main entry point for getting content based on user intent.
 */
class ContentRepository(
    private val ai: LucidAI,
    private val fetcher: ContentFetcher = ContentFetcher(),
    private val parser: ContentParser = ContentParser()
) {

    /**
     * Get content based on classified intent
     */
    suspend fun getContent(intent: IntentClassification): ContentResult<Any> {
        return when (intent.intentType) {
            IntentType.RECIPE -> getRecipe(intent.topic)
            IntentType.DEFINITION -> getDefinition(intent.topic)
            IntentType.LEARN -> getLearnContent(intent.topic)
            IntentType.SEARCH -> searchContent(intent.topic)
            IntentType.NAVIGATE -> ContentResult.Success(
                ExtractedContent(
                    title = "Navigate",
                    summary = intent.topic,
                    content = intent.topic,
                    source = "URL"
                )
            )
            IntentType.CALCULATE -> calculateResult(intent.topic)
            IntentType.WEATHER -> getWeather(intent.topic)
            IntentType.NEWS -> getNews(intent.topic)
            IntentType.UNKNOWN -> searchContent(intent.topic)
        }
    }

    /**
     * Get a recipe for a dish
     */
    suspend fun getRecipe(query: String): ContentResult<Recipe> {
        return withContext(Dispatchers.IO) {
            // Use TheMealDB API directly
            val recipeResult = fetcher.searchRecipes(query)

            if (recipeResult is ContentResult.Success) {
                return@withContext recipeResult
            }

            // If specific search fails, try getting a random recipe as fallback
            // (better UX than showing error)
            val randomResult = fetcher.getRandomRecipe()
            if (randomResult is ContentResult.Success) {
                // Add note that this is a suggested recipe
                val recipe = randomResult.data
                return@withContext ContentResult.Success(
                    recipe.copy(
                        description = "Suggested recipe: ${recipe.description}".trim()
                    )
                )
            }

            ContentResult.Error("Could not find recipe for \"$query\". Try searching for common dishes like \"chicken\", \"pasta\", or \"soup\".")
        }
    }

    /**
     * Get definition for a term
     */
    suspend fun getDefinition(term: String): ContentResult<Definition> {
        return withContext(Dispatchers.IO) {
            // Try Wikipedia first
            val wikiResult = fetcher.fetchWikipediaSummary(term)
            if (wikiResult is ContentResult.Success) {
                return@withContext wikiResult
            }

            // Fallback to DuckDuckGo
            val ddgResult = fetcher.fetchDuckDuckGoAnswer("define $term")
            if (ddgResult is ContentResult.Success) {
                return@withContext ContentResult.Success(
                    Definition(
                        term = term,
                        definition = ddgResult.data.summary,
                        source = ddgResult.data.source
                    )
                )
            }

            ContentResult.Error("Could not find definition for $term")
        }
    }

    /**
     * Get learning content for a topic
     */
    suspend fun getLearnContent(topic: String): ContentResult<LearnContent> {
        return withContext(Dispatchers.IO) {
            // Get Wikipedia article
            val wikiResult = fetcher.fetchWikipediaSummary(topic)

            if (wikiResult is ContentResult.Success) {
                val definition = wikiResult.data

                // Extract key facts using AI
                val keyFacts = ai.extractKeyPoints(definition.definition, 5)

                return@withContext ContentResult.Success(
                    LearnContent(
                        title = definition.term,
                        summary = ai.summarize(definition.definition, 200),
                        sections = listOf(
                            ContentSection("Overview", definition.definition)
                        ),
                        keyFacts = keyFacts,
                        source = definition.source
                    )
                )
            }

            // Fallback to DuckDuckGo
            val ddgResult = fetcher.fetchDuckDuckGoAnswer(topic)
            if (ddgResult is ContentResult.Success) {
                return@withContext ContentResult.Success(
                    LearnContent(
                        title = ddgResult.data.title,
                        summary = ddgResult.data.summary,
                        sections = listOf(
                            ContentSection("Overview", ddgResult.data.content)
                        ),
                        source = ddgResult.data.source
                    )
                )
            }

            ContentResult.Error("Could not find information about $topic")
        }
    }

    /**
     * General search
     */
    suspend fun searchContent(query: String): ContentResult<ExtractedContent> {
        return withContext(Dispatchers.IO) {
            // Try DuckDuckGo instant answer
            val ddgResult = fetcher.fetchDuckDuckGoAnswer(query)
            if (ddgResult is ContentResult.Success) {
                return@withContext ddgResult
            }

            // Try Wikipedia
            val wikiResult = fetcher.fetchWikipediaSummary(query)
            if (wikiResult is ContentResult.Success) {
                return@withContext ContentResult.Success(
                    ExtractedContent(
                        title = wikiResult.data.term,
                        summary = wikiResult.data.definition,
                        content = wikiResult.data.definition,
                        source = wikiResult.data.source
                    )
                )
            }

            ContentResult.Error("No results found for $query")
        }
    }

    /**
     * Calculate or convert
     */
    private suspend fun calculateResult(expression: String): ContentResult<ExtractedContent> {
        return withContext(Dispatchers.Default) {
            // Simple calculation support
            try {
                val result = evaluateExpression(expression)
                if (result != null) {
                    return@withContext ContentResult.Success(
                        ExtractedContent(
                            title = "Calculation",
                            summary = "$expression = $result",
                            content = "$expression = $result",
                            source = "Calculator"
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore calculation errors
            }

            // Fallback to DuckDuckGo for conversions
            fetcher.fetchDuckDuckGoAnswer(expression)
        }
    }

    /**
     * Get weather (placeholder - would need weather API)
     */
    private suspend fun getWeather(location: String): ContentResult<ExtractedContent> {
        return fetcher.fetchDuckDuckGoAnswer("weather $location")
    }

    /**
     * Get news (placeholder - would need news API)
     */
    private suspend fun getNews(topic: String): ContentResult<ExtractedContent> {
        return fetcher.fetchDuckDuckGoAnswer("$topic news today")
    }

    /**
     * Simple expression evaluator
     */
    private fun evaluateExpression(expr: String): Double? {
        return try {
            val cleaned = expr.replace(Regex("[^0-9+\\-*/().\\s]"), "")
                .replace(" ", "")

            if (cleaned.isEmpty()) return null

            // Very simple evaluation - only handles basic operations
            // For a real app, use a proper expression parser
            val result = cleaned.toDoubleOrNull()
            result
        } catch (e: Exception) {
            null
        }
    }
}
