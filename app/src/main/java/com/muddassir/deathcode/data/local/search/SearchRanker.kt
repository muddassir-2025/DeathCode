package com.muddassir.deathcode.data.local.search

import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.core.TextNormalizer
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity

/**
 * Transparent weighted ranking. Weights are ordered exactly like the product spec:
 * exact title, exact keyword, title prefix, exact phrase, syntax, note, markdown, category,
 * then fuzzy similarity as the weakest signal.
 *
 * Kept as a pure object so it is trivially unit testable.
 */
object SearchRanker {

    private const val EXACT_TITLE = 100.0
    private const val EXACT_TITLE_TOKEN = 70.0
    private const val TITLE_PREFIX = 55.0
    private const val TITLE_CONTAINS = 40.0
    private const val TITLE_TOKEN_PREFIX = 30.0
    private const val EXACT_KEYWORD = 60.0
    private const val KEYWORD_PREFIX = 32.0
    private const val SYNTAX_MATCH = 22.0
    private const val NOTE_MATCH = 16.0
    private const val CONTENT_MATCH = 10.0
    private const val CATEGORY_MATCH = 8.0
    private const val FUZZY_MAX = 25.0
    private const val EXACT_PHRASE_BONUS = 45.0

    fun rank(
        query: ProcessedQuery,
        node: ContentNodeEntity,
        override: PrivateOverrideEntity?,
        path: List<String>,
    ): SearchResult? {
        val titleNorm = TextNormalizer.normalize(node.title)
        val titleTokens = titleNorm.split(' ').filter { it.isNotEmpty() }
        val keywords = (
            KeywordList.parse(node.keywords) + KeywordList.parse(override?.keywords)
            ).distinct()
        val syntaxText = TextNormalizer.normalize(
            listOfNotNull(node.syntax, override?.syntax, override?.template).joinToString(" "),
        )
        val notesText = TextNormalizer.normalize(
            listOfNotNull(node.notes, override?.note).joinToString(" "),
        )
        val contentText = TextNormalizer.normalize(node.markdown)
        val categoryText = TextNormalizer.normalize(path.dropLast(1).joinToString(" "))

        var score = 0.0
        val matched = linkedSetOf<MatchField>()

        for (term in query.terms) {
            when {
                titleNorm == term -> {
                    score += EXACT_TITLE
                    matched += MatchField.TITLE
                }
                titleTokens.any { it == term } -> {
                    score += EXACT_TITLE_TOKEN
                    matched += MatchField.TITLE
                }
                titleNorm.startsWith(term) -> {
                    score += TITLE_PREFIX
                    matched += MatchField.TITLE
                }
                titleNorm.contains(term) -> {
                    score += TITLE_CONTAINS
                    matched += MatchField.TITLE
                }
                titleTokens.any { it.startsWith(term) } -> {
                    score += TITLE_TOKEN_PREFIX
                    matched += MatchField.TITLE
                }
            }

            when {
                keywords.any { it == term } -> {
                    score += EXACT_KEYWORD
                    matched += MatchField.KEYWORD
                }
                keywords.any { it.startsWith(term) } -> {
                    score += KEYWORD_PREFIX
                    matched += MatchField.KEYWORD
                }
            }

            if (syntaxText.contains(term)) {
                score += SYNTAX_MATCH
                matched += MatchField.SYNTAX
            }
            if (notesText.contains(term)) {
                score += NOTE_MATCH
                matched += MatchField.NOTE
            }
            if (contentText.contains(term)) {
                score += CONTENT_MATCH
                matched += MatchField.CONTENT
            }
            if (categoryText.contains(term)) {
                score += CATEGORY_MATCH
                matched += MatchField.CATEGORY
            }

            // Fuzzy is only a weak fallback signal, and only when the stronger signals
            // did not already match this term.
            if (MatchField.TITLE !in matched && MatchField.KEYWORD !in matched) {
                val best = titleTokens.maxOfOrNull { token ->
                    if (TextNormalizer.isFuzzyMatch(term, token)) {
                        val longest = maxOf(term.length, token.length)
                        1.0 - TextNormalizer.levenshtein(term, token).toDouble() / longest
                    } else {
                        0.0
                    }
                } ?: 0.0
                if (best > 0.0) {
                    score += FUZZY_MAX * best
                    matched += MatchField.FUZZY
                }
            }
        }

        val normalizedQuery = TextNormalizer.normalize(query.original)
        if (normalizedQuery.length > 3 && titleNorm.contains(normalizedQuery)) {
            score += EXACT_PHRASE_BONUS
            matched += MatchField.TITLE
        }

        if (score <= 0.0) return null

        return SearchResult(
            node = node,
            path = path,
            score = score,
            matchedFields = matched.toList(),
            snippet = snippetFor(node, override),
        )
    }

    private fun snippetFor(
        node: ContentNodeEntity,
        override: PrivateOverrideEntity?,
    ): String? {
        override?.template?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        node.syntax?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        return node.markdown
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
    }
}
