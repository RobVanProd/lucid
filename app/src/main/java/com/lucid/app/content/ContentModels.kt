package com.lucid.app.content

/**
 * Content models for LUCID
 */

/**
 * Extracted content from a web page or API
 */
data class ExtractedContent(
    val title: String,
    val summary: String,
    val content: String,
    val keyPoints: List<String> = emptyList(),
    val source: String = "",
    val imageUrl: String? = null
)

/**
 * Recipe data model
 */
data class Recipe(
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val steps: List<RecipeStep>,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val servings: String? = null,
    val imageUrl: String? = null,
    val source: String = ""
)

data class RecipeStep(
    val number: Int,
    val instruction: String,
    val duration: String? = null
)

/**
 * Definition/Wiki data model
 */
data class Definition(
    val term: String,
    val definition: String,
    val additionalInfo: List<String> = emptyList(),
    val relatedTerms: List<String> = emptyList(),
    val source: String = ""
)

/**
 * Learning content data model
 */
data class LearnContent(
    val title: String,
    val summary: String,
    val sections: List<ContentSection>,
    val keyFacts: List<String> = emptyList(),
    val source: String = ""
)

data class ContentSection(
    val heading: String,
    val content: String
)

/**
 * Result wrapper for content operations
 */
sealed class ContentResult<out T> {
    data class Success<T>(val data: T) : ContentResult<T>()
    data class Error(val message: String) : ContentResult<Nothing>()
    data object Loading : ContentResult<Nothing>()
}
