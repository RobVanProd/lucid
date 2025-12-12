package com.lucid.app.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

/**
 * Content Parser
 *
 * Extracts clean content from HTML using Jsoup.
 * Removes ads, navigation, and other noise.
 */
class ContentParser {

    /**
     * Extract main content from HTML
     */
    suspend fun extractContent(html: String, url: String = ""): ExtractedContent {
        return withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(html)

            // Remove noise elements
            removeNoiseElements(doc)

            // Get title
            val title = doc.select("h1").firstOrNull()?.text()
                ?: doc.title()
                ?: "Content"

            // Try to find main content
            val mainContent = findMainContent(doc)

            // Extract key points (headings or list items)
            val keyPoints = extractKeyPoints(doc)

            // Get summary (first paragraph or meta description)
            val summary = doc.select("meta[name=description]").attr("content")
                .ifEmpty { mainContent.take(300) }

            ExtractedContent(
                title = title,
                summary = summary,
                content = mainContent,
                keyPoints = keyPoints,
                source = extractDomain(url)
            )
        }
    }

    /**
     * Parse recipe from HTML
     */
    suspend fun parseRecipe(html: String, url: String = ""): Recipe? {
        return withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(html)

            // Try JSON-LD first (structured data)
            val jsonLd = doc.select("script[type=application/ld+json]")
            for (script in jsonLd) {
                val jsonText = script.data()
                if (jsonText.contains("\"Recipe\"") || jsonText.contains("\"@type\":\"Recipe\"")) {
                    val recipe = parseRecipeJsonLd(jsonText)
                    if (recipe != null) return@withContext recipe.copy(source = extractDomain(url))
                }
            }

            // Fallback: try common recipe selectors
            removeNoiseElements(doc)

            val title = doc.select("h1").firstOrNull()?.text()
                ?: doc.select(".recipe-title, .wprm-recipe-name").firstOrNull()?.text()
                ?: "Recipe"

            // Find ingredients
            val ingredients = mutableListOf<String>()
            doc.select(".ingredient, .ingredients li, .wprm-recipe-ingredient, [itemprop=recipeIngredient]")
                .forEach { el ->
                    val text = el.text().trim()
                    if (text.isNotEmpty() && text.length < 200) {
                        ingredients.add(text)
                    }
                }

            // Find instructions
            val steps = mutableListOf<RecipeStep>()
            var stepNum = 1
            doc.select(".instruction, .instructions li, .wprm-recipe-instruction, [itemprop=recipeInstructions] li, .recipe-directions li")
                .forEach { el ->
                    val text = el.text().trim()
                    if (text.isNotEmpty() && text.length > 10) {
                        steps.add(RecipeStep(stepNum++, text))
                    }
                }

            // If we found ingredients or steps, return the recipe
            if (ingredients.isNotEmpty() || steps.isNotEmpty()) {
                val description = doc.select("meta[name=description]").attr("content")
                    .ifEmpty {
                        doc.select(".recipe-summary, .wprm-recipe-summary, [itemprop=description]")
                            .firstOrNull()?.text() ?: ""
                    }

                return@withContext Recipe(
                    title = title,
                    description = description,
                    ingredients = ingredients,
                    steps = steps,
                    source = extractDomain(url)
                )
            }

            null
        }
    }

    /**
     * Parse recipe from JSON-LD structured data
     */
    private fun parseRecipeJsonLd(jsonText: String): Recipe? {
        try {
            // Simple JSON parsing without full library
            val title = extractJsonValue(jsonText, "name") ?: return null

            val ingredientsMatch = Regex("\"recipeIngredient\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
                .find(jsonText)
            val ingredients = ingredientsMatch?.groupValues?.get(1)
                ?.split(",")
                ?.mapNotNull { it.trim().removeSurrounding("\"").takeIf { s -> s.isNotEmpty() } }
                ?: emptyList()

            val instructionsMatch = Regex("\"recipeInstructions\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
                .find(jsonText)
            val instructionsRaw = instructionsMatch?.groupValues?.get(1) ?: ""

            val steps = mutableListOf<RecipeStep>()
            var stepNum = 1

            // Try to parse HowToStep format
            val stepMatches = Regex("\"text\"\\s*:\\s*\"(.*?)\"").findAll(instructionsRaw)
            stepMatches.forEach { match ->
                val instruction = match.groupValues[1]
                    .replace("\\n", " ")
                    .replace("\\\"", "\"")
                    .trim()
                if (instruction.isNotEmpty()) {
                    steps.add(RecipeStep(stepNum++, instruction))
                }
            }

            // Fallback: simple string array
            if (steps.isEmpty()) {
                instructionsRaw.split(",")
                    .mapNotNull { it.trim().removeSurrounding("\"").takeIf { s -> s.length > 10 } }
                    .forEach { instruction ->
                        steps.add(RecipeStep(stepNum++, instruction))
                    }
            }

            val description = extractJsonValue(jsonText, "description") ?: ""
            val prepTime = extractJsonValue(jsonText, "prepTime")
            val cookTime = extractJsonValue(jsonText, "cookTime")

            return Recipe(
                title = title,
                description = description,
                ingredients = ingredients,
                steps = steps,
                prepTime = formatDuration(prepTime),
                cookTime = formatDuration(cookTime)
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun extractJsonValue(json: String, key: String): String? {
        val match = Regex("\"$key\"\\s*:\\s*\"(.*?)\"").find(json)
        return match?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
    }

    private fun formatDuration(isoDuration: String?): String? {
        if (isoDuration == null) return null
        // Parse ISO 8601 duration (PT30M, PT1H30M, etc.)
        val hours = Regex("(\\d+)H").find(isoDuration)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = Regex("(\\d+)M").find(isoDuration)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> null
        }
    }

    private fun removeNoiseElements(doc: Document) {
        // Remove common noise selectors
        val noiseSelectors = listOf(
            "script", "style", "noscript", "iframe",
            "nav", "header", "footer", "aside",
            ".ad", ".ads", ".advertisement", ".sponsored",
            ".sidebar", ".widget", ".social", ".share",
            ".comment", ".comments", ".related", ".recommended",
            ".newsletter", ".subscribe", ".popup", ".modal",
            "[role=banner]", "[role=navigation]", "[role=complementary]"
        )

        noiseSelectors.forEach { selector ->
            doc.select(selector).remove()
        }
    }

    private fun findMainContent(doc: Document): String {
        // Try semantic elements first
        val mainSelectors = listOf(
            "article",
            "[role=main]",
            "main",
            ".post-content",
            ".article-content",
            ".entry-content",
            ".content",
            "#content"
        )

        for (selector in mainSelectors) {
            val element = doc.select(selector).firstOrNull()
            if (element != null) {
                val text = Jsoup.clean(element.html(), Safelist.none())
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (text.length > 100) {
                    return text
                }
            }
        }

        // Fallback: get body text
        return doc.body()?.text()?.take(5000) ?: ""
    }

    private fun extractKeyPoints(doc: Document): List<String> {
        val points = mutableListOf<String>()

        // Get h2/h3 headings as key points
        doc.select("h2, h3").take(5).forEach { heading ->
            val text = heading.text().trim()
            if (text.length in 5..100) {
                points.add(text)
            }
        }

        // If no headings, try list items
        if (points.isEmpty()) {
            doc.select("ul li, ol li").take(5).forEach { li ->
                val text = li.text().trim()
                if (text.length in 10..150) {
                    points.add(text)
                }
            }
        }

        return points
    }

    private fun extractDomain(url: String): String {
        return try {
            val domain = url.removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .substringBefore("/")
            domain.ifEmpty { "Web" }
        } catch (e: Exception) {
            "Web"
        }
    }
}
