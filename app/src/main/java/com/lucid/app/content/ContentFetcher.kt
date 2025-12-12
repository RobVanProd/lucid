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
     * Search for recipes using DuckDuckGo
     */
    suspend fun searchRecipes(query: String): ContentResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Search for recipes on AllRecipes or similar
                val searchQuery = "$query recipe site:allrecipes.com OR site:simplyrecipes.com"
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext ContentResult.Error("No results")

                ContentResult.Success(body)
            } catch (e: Exception) {
                ContentResult.Error(e.message ?: "Recipe search failed")
            }
        }
    }
}
