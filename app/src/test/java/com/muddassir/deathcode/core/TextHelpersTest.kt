package com.muddassir.deathcode.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextHelpersTest {

    @Test
    fun `normalize lowercases and collapses punctuation but keeps identifiers`() {
        assertEquals("push_back vector", TextNormalizer.normalize("push_back(vector)"))
        assertEquals("lower_bound", TextNormalizer.normalize("lower_bound()"))
        assertEquals("", TextNormalizer.normalize("   !!!   "))
    }

    @Test
    fun `tokenize splits on normalized boundaries`() {
        assertEquals(
            listOf("decimal", "to", "binary"),
            TextNormalizer.tokenize("Decimal to Binary"),
        )
        // Operators are separators, not tokens.
        assertEquals(
            listOf("decimal", "binary"),
            TextNormalizer.tokenize("Decimal -> Binary"),
        )
    }

    @Test
    fun `levenshtein measures single character typos`() {
        assertEquals(1, TextNormalizer.levenshtein("binry", "binary"))
        assertEquals(0, TextNormalizer.levenshtein("BFS", "bfs"))
    }

    @Test
    fun `fuzzy matching tolerates small typos and rejects unrelated words`() {
        assertTrue(TextNormalizer.isFuzzyMatch("binry", "binary"))
        assertTrue(TextNormalizer.isFuzzyMatch("pyton", "python"))
        assertFalse(TextNormalizer.isFuzzyMatch("cooking", "binary"))
        assertFalse(TextNormalizer.isFuzzyMatch("ab", "abc"))
    }

    @Test
    fun `similarity is bounded and symmetric`() {
        val forward = TextNormalizer.similarity("binary", "binry")
        val backward = TextNormalizer.similarity("binry", "binary")

        assertTrue(forward in 0.0..1.0)
        assertEquals(forward, backward, 0.0001)
    }

    @Test
    fun `keyword list parses, dedupes and serializes`() {
        val parsed = KeywordList.parse("For, forloop , LOOP, for")

        assertEquals(listOf("for", "forloop", "loop"), parsed)
        assertEquals("for,forloop,loop", KeywordList.serialize(parsed))
        assertEquals("", KeywordList.serialize(emptyList()))
    }

    @Test
    fun `slugify produces stable ids`() {
        assertEquals("for-loop", slugify("For Loop"))
        assertEquals("c-built-in-functions", slugify("C++ Built-in Functions"))
        assertEquals("node", slugify("!!!"))
        assertEquals("node", slugify(""))
    }
}
