package com.muddassir.deathcode.core

import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic text helpers shared by the search index, the query processor and the
 * keyboard suggestion engine. No AI, no network — just string math.
 */
object TextNormalizer {

    /**
     * Lowercases and replaces every non alphanumeric run with a single space.
     *
     * `_` is treated as a word character so programming identifiers stay intact
     * (`push_back`, `lower_bound`), which keeps their prefix matching useful.
     */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        var lastWasSpace = true
        for (ch in text.lowercase()) {
            if (ch.isLetterOrDigit() || ch == '_') {
                sb.append(ch)
                lastWasSpace = false
            } else if (!lastWasSpace) {
                sb.append(' ')
                lastWasSpace = true
            }
        }
        return sb.toString().trim()
    }

    fun tokenize(text: String): List<String> =
        normalize(text).split(' ').filter { it.isNotEmpty() }

    /** Strips characters that are meaningful to the FTS query grammar. */
    fun ftsToken(token: String): String =
        token.filter { it.isLetterOrDigit() || it == '_' }

    /** Sørensen–Dice coefficient over character bigrams, in `0.0..1.0`. */
    fun similarity(a: String, b: String): Double {
        val x = normalize(a).replace(" ", "")
        val y = normalize(b).replace(" ", "")
        if (x.isEmpty() || y.isEmpty()) return 0.0
        if (x == y) return 1.0
        if (x.length < 2 || y.length < 2) return if (x == y) 1.0 else 0.0

        val aGrams = HashMap<String, Int>()
        for (i in 0 until x.length - 1) {
            aGrams.merge(x.substring(i, i + 2), 1, Int::plus)
        }
        var intersection = 0
        for (i in 0 until y.length - 1) {
            val gram = y.substring(i, i + 2)
            val count = aGrams[gram] ?: continue
            if (count > 0) {
                aGrams[gram] = count - 1
                intersection++
            }
        }
        return (2.0 * intersection) / ((x.length - 1) + (y.length - 1))
    }

    /** Bounded Levenshtein distance used for small typo tolerance. */
    fun levenshtein(a: String, b: String): Int {
        val x = normalize(a)
        val y = normalize(b)
        if (x == y) return 0
        if (x.isEmpty()) return y.length
        if (y.isEmpty()) return x.length
        var previous = IntArray(y.length + 1) { it }
        var current = IntArray(y.length + 1)
        for (i in 1..x.length) {
            current[0] = i
            for (j in 1..y.length) {
                val cost = if (x[i - 1] == y[j - 1]) 0 else 1
                current[j] = min(min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[y.length]
    }

    /** True when [term] looks like a typo of [candidate] within a proportional threshold. */
    fun isFuzzyMatch(term: String, candidate: String, threshold: Double = 0.66): Boolean {
        if (term.length < 3) return false
        val distance = levenshtein(term, candidate)
        val longest = max(term.length, candidate.length)
        return 1.0 - distance.toDouble() / longest >= threshold
    }
}
