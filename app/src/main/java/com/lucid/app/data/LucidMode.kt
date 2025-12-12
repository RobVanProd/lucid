package com.lucid.app.data

/**
 * The two states of consciousness in LUCID
 */
enum class LucidMode {
    /**
     * LUCID Mode - The filter is up.
     * Monochrome, distraction-free, intention-focused.
     * The dopamine loops are severed.
     */
    LUCID,

    /**
     * EXPLORE Mode - The internet as it is.
     * Raw, unfiltered, full color.
     * Use with awareness.
     */
    EXPLORE
}

/**
 * Represents a communication item in the briefing
 */
data class BriefingItem(
    val id: String,
    val source: String,
    val sender: String,
    val preview: String,
    val timestamp: Long,
    val priority: Priority
)

enum class Priority {
    URGENT,
    IMPORTANT,
    NORMAL,
    LOW
}

/**
 * Represents the current user intention
 */
data class Intention(
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
