package com.lucid.app.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Content Fetcher
 *
 * Fetches content from various sources (Wikipedia, recipe sites, etc.)
 * and returns raw HTML/JSON for parsing.
 */
class ContentFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent = "LUCID/0.2 (Android; Cognitive Firewall)"

    /**
     * Fetch Wikipedia summary for a topic
     */
    suspend fun fetchWikipediaSummary(topic: String): ContentResult<Definition> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedTopic = URLEncoder.encode(topic, "UTF-8")
                val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedTopic"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext ContentResult.Error("Wikipedia not found")
                }

                val body = response.body?.string() ?: return@withContext ContentResult.Error("Empty response")
                val json = JSONObject(body)

                val title = json.optString("title", topic)
                val extract = json.optString("extract", "")
                val description = json.optString("description", "")

                if (extract.isEmpty()) {
                    return@withContext ContentResult.Error("No content found")
                }

                ContentResult.Success(
                    Definition(
                        term = title,
                        definition = extract,
                        additionalInfo = if (description.isNotEmpty()) listOf(description) else emptyList(),
                        source = "Wikipedia"
                    )
                )
            } catch (e: Exception) {
                ContentResult.Error(e.message ?: "Failed to fetch from Wikipedia")
            }
        }
    }

    /**
     * Fetch DuckDuckGo instant answer
     */
    suspend fun fetchDuckDuckGoAnswer(query: String): ContentResult<ExtractedContent> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext ContentResult.Error("Search failed")
                }

                val body = response.body?.string() ?: return@withContext ContentResult.Error("Empty response")
                val json = JSONObject(body)

                // Try abstract first
                val abstractText = json.optString("AbstractText", "")
                val heading = json.optString("Heading", query)
                val abstractSource = json.optString("AbstractSource", "")

                if (abstractText.isNotEmpty()) {
                    return@withContext ContentResult.Success(
                        ExtractedContent(
                            title = heading,
                            summary = abstractText,
                            content = abstractText,
                            source = abstractSource
                        )
                    )
                }

                // Try answer
                val answer = json.optString("Answer", "")
                if (answer.isNotEmpty()) {
                    return@withContext ContentResult.Success(
                        ExtractedContent(
                            title = heading,
                            summary = answer,
                            content = answer,
                            source = "DuckDuckGo"
                        )
                    )
                }

                // Try definition
                val definition = json.optString("Definition", "")
                if (definition.isNotEmpty()) {
                    return@withContext ContentResult.Success(
                        ExtractedContent(
                            title = heading,
                            summary = definition,
                            content = definition,
                            source = json.optString("DefinitionSource", "")
                        )
                    )
                }

                ContentResult.Error("No direct answer available")
            } catch (e: Exception) {
                ContentResult.Error(e.message ?: "Search failed")
            }
        }
    }

    /**
     * Fetch raw HTML from a URL
     */
    suspend fun fetchHtml(url: String): ContentResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext ContentResult.Error("Failed to load page")
                }

                val body = response.body?.string() ?: return@withContext ContentResult.Error("Empty page")
                ContentResult.Success(body)
            } catch (e: Exception) {
                ContentResult.Error(e.message ?: "Failed to fetch page")
            }
        }
    }

    /**
     * Search for recipes using TheMealDB API (free, no API key needed)
     */
    suspend fun searchRecipes(query: String): ContentResult<Recipe> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://www.themealdb.com/api/json/v1/1/search.php?s=$encodedQuery"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext ContentResult.Error("Recipe search failed")
                }

                val body = response.body?.string() ?: return@withContext ContentResult.Error("Empty response")
                val json = JSONObject(body)

                val meals = json.optJSONArray("meals")
                if (meals == null || meals.length() == 0) {
                    return@withContext ContentResult.Error("No recipes found")
                }

                val meal = meals.getJSONObject(0)
                val recipe = parseMealDbRecipe(meal)
                ContentResult.Success(recipe)
            } catch (e: Exception) {
                ContentResult.Error(e.message ?: "Recipe search failed")
            }
        }
    }

    /**
     * Get a random recipe from TheMealDB
     */
    suspend fun getRandomRecipe(): ContentResult<Recipe> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.themealdb.com/api/json/v1/1/random.php"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext ContentResult.Error("Empty response")
                val json = JSONObject(body)

                val meals = json.optJSONArray("meals")
                if (meals == null || meals.length() == 0) {
                    return@withContext ContentResult.Error("No recipe found")
                }

                val meal = meals.getJSONObject(0)
                val recipe = parseMealDbRecipe(meal)
                ContentResult.Success(recipe)
            } catch (e: Exception) {
                ContentResult.Error(e.message ?: "Recipe fetch failed")
            }
        }
    }

    /**
     * Parse TheMealDB JSON into Recipe object
     */
    private fun parseMealDbRecipe(meal: JSONObject): Recipe {
        val title = meal.optString("strMeal", "Recipe")
        val category = meal.optString("strCategory", "")
        val area = meal.optString("strArea", "")
        val instructions = meal.optString("strInstructions", "")

        // Extract ingredients (TheMealDB uses strIngredient1-20 and strMeasure1-20)
        val ingredients = mutableListOf<String>()
        for (i in 1..20) {
            val ingredient = meal.optString("strIngredient$i", "").trim()
            val measure = meal.optString("strMeasure$i", "").trim()
            if (ingredient.isNotEmpty()) {
                val full = if (measure.isNotEmpty()) "$measure $ingredient" else ingredient
                ingredients.add(full)
            }
        }

        // Parse instructions into steps
        val steps = instructions
            .split(Regex("\\r?\\n|\\. (?=[A-Z])"))
            .filter { it.trim().length > 10 }
            .mapIndexed { index, instruction ->
                RecipeStep(index + 1, instruction.trim().removeSuffix(".") + ".")
            }

        val description = buildString {
            if (category.isNotEmpty()) append("$category")
            if (area.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append("$area cuisine")
            }
        }

        return Recipe(
            title = title,
            description = description,
            ingredients = ingredients,
            steps = steps,
            source = "TheMealDB"
        )
    }
}
