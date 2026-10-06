package com.muddassir.deathcode.data.local.search

import com.muddassir.deathcode.core.TextNormalizer

/**
 * The result of turning a raw user query into search terms.
 *
 * ```
 * "How can I convert a decimal number into a binary number?"
 *   -> original : the raw query
 *   -> tokens   : [how, can, i, convert, a, decimal, number, into, a, binary, number]
 *   -> terms    : [convert, decimal, number, binary]
 *   -> ftsMatch : convert* OR decimal* OR number* OR binary*
 * ```
 */
data class ProcessedQuery(
    val original: String,
    val tokens: List<String>,
    val terms: List<String>,
    val ftsMatch: String?,
) {
    val isEmpty: Boolean get() = terms.isEmpty()
}

/**
 * Deterministic, fully local query processing.
 *
 * No LLM, no embeddings, no cloud semantic search: the query is normalized, tokenized,
 * stripped of common English function words and compiled into an FTS4 match expression
 * with prefix matching.
 */
object QueryProcessor {

    /** English function words that carry no programming meaning. */
    private val STOP_WORDS = setOf(
        "a", "an", "the", "how", "can", "could", "do", "does", "did", "i", "me", "my",
        "we", "you", "your", "is", "are", "was", "were", "be", "to", "into", "in", "on",
        "of", "for", "with", "using", "use", "used", "and", "or", "but", "what", "which",
        "when", "where", "why", "who", "that", "this", "these", "those", "it", "its",
        "as", "at", "by", "from", "so", "if", "then", "than", "there", "here", "please",
        "want", "need", "give", "show", "tell", "some", "any", "all", "about", "just",
    )

    fun process(rawQuery: String): ProcessedQuery {
        val tokens = TextNormalizer.tokenize(rawQuery)
        val terms = tokens
            .filter { it.length > 1 || it.any(Char::isDigit) }
            .filter { it !in STOP_WORDS }
            .distinct()

        // If every token was a stop word, fall back to the raw tokens so the user still
        // gets a result instead of an empty screen.
        val effective = terms.ifEmpty { tokens.distinct() }

        val fts = effective
            .map(TextNormalizer::ftsToken)
            .filter { it.isNotEmpty() }
            .joinToString(" OR ") { "$it*" }
            .ifEmpty { null }

        return ProcessedQuery(
            original = rawQuery,
            tokens = tokens,
            terms = effective,
            ftsMatch = fts,
        )
    }
}
