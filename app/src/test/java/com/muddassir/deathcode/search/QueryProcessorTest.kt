package com.muddassir.deathcode.search

import com.muddassir.deathcode.data.local.search.QueryProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryProcessorTest {

    @Test
    fun `drops common English function words`() {
        val processed = QueryProcessor.process("How can I convert a decimal number into a binary number?")

        assertEquals(listOf("convert", "decimal", "number", "binary"), processed.terms)
    }

    @Test
    fun `builds a prefix FTS expression`() {
        val processed = QueryProcessor.process("decimal to binary")

        assertEquals("decimal* OR binary*", processed.ftsMatch)
    }

    @Test
    fun `keeps the query usable when every token is a stop word`() {
        val processed = QueryProcessor.process("how do i")

        assertEquals(listOf("how", "do", "i"), processed.terms)
        assertTrue(processed.ftsMatch!!.contains("how*"))
    }

    @Test
    fun `strips characters that would break the FTS grammar`() {
        val processed = QueryProcessor.process("push_back()")

        assertEquals("push_back*", processed.ftsMatch)
    }

    @Test
    fun `treats a blank query as empty`() {
        val processed = QueryProcessor.process("   ")

        assertTrue(processed.isEmpty)
        assertEquals(null, processed.ftsMatch)
    }

    @Test
    fun `keeps short but meaningful tokens containing digits`() {
        val processed = QueryProcessor.process("c++ 2d array")

        assertTrue(processed.terms.contains("2d"))
        assertTrue(processed.terms.contains("array"))
    }
}
