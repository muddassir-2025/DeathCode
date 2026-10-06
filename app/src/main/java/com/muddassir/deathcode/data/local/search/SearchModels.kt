package com.muddassir.deathcode.data.local.search

import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity

/** Which indexed field produced a search hit. */
enum class MatchField {
    TITLE,
    KEYWORD,
    SYNTAX,
    NOTE,
    CONTENT,
    CATEGORY,
    FUZZY,
}

/**
 * A ranked search hit, ready for display.
 *
 * Carries enough context for the UI to show *where* the content lives
 * (`C++ > DSA > Fundamentals > Loops`) and *why* it matched (`Title · Keyword · Syntax`).
 */
data class SearchResult(
    val node: ContentNodeEntity,
    val path: List<String>,
    val score: Double,
    val matchedFields: List<MatchField>,
    val snippet: String? = null,
) {
    val breadcrumb: String get() = path.joinToString(" > ")
}

/** Which local data sources a search should consider. */
data class SearchFilters(
    val sources: Set<com.muddassir.deathcode.domain.model.ContentSource> =
        setOf(
            com.muddassir.deathcode.domain.model.ContentSource.OFFICIAL,
            com.muddassir.deathcode.domain.model.ContentSource.PRIVATE,
        ),
) {
    fun includes(source: com.muddassir.deathcode.domain.model.ContentSource): Boolean =
        source in sources
}
