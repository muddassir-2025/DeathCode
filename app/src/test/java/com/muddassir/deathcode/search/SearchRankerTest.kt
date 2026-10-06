package com.muddassir.deathcode.search

import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity
import com.muddassir.deathcode.data.local.search.MatchField
import com.muddassir.deathcode.data.local.search.QueryProcessor
import com.muddassir.deathcode.data.local.search.SearchRanker
import com.muddassir.deathcode.domain.model.ContentSource
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankerTest {

    private fun node(
        id: String,
        title: String,
        markdown: String = "",
        syntax: String? = null,
        notes: String? = null,
        keywords: String = "",
    ) = ContentNodeEntity(
        id = id,
        parentId = null,
        title = title,
        slug = id,
        markdown = markdown,
        syntax = syntax,
        notes = notes,
        keywords = keywords,
        source = ContentSource.OFFICIAL,
    )

    private fun rank(
        query: String,
        node: ContentNodeEntity,
        override: PrivateOverrideEntity? = null,
        path: List<String> = listOf("C++", "DSA"),
    ) = SearchRanker.rank(QueryProcessor.process(query), node, override, path)

    @Test
    fun `exact title match outranks a body match`() {
        val exact = rank("for loop", node("1", "For Loop", markdown = "unrelated"))!!.score
        val body = rank("for loop", node("2", "Iteration", markdown = "the for loop repeats"))!!.score

        assertTrue(exact > body)
    }

    @Test
    fun `reports which fields matched`() {
        val result = rank(
            "binary",
            node("1", "Decimal to Binary", syntax = "n >>= 1", keywords = "binary,decimal"),
        )!!

        assertTrue(result.matchedFields.contains(MatchField.TITLE))
        assertTrue(result.matchedFields.contains(MatchField.KEYWORD))
    }

    @Test
    fun `private note makes official content searchable`() {
        val override = PrivateOverrideEntity(
            id = "ov1",
            nodeId = "1",
            note = "remember the off by one bug",
            template = "for (long long i = 0; i < n; i++) {}",
        )

        val without = rank("off by one", node("1", "For Loop"))
        val with = rank("off by one", node("1", "For Loop"), override)

        assertNull(without)
        assertTrue(with != null && with.matchedFields.contains(MatchField.NOTE))
    }

    @Test
    fun `category context is a weak signal`() {
        val result = rank(
            "dsa",
            node("1", "Some Topic", markdown = "nothing"),
            path = listOf("DSA", "Some Topic"),
        )!!

        assertTrue(result.matchedFields.contains(MatchField.CATEGORY))
        assertTrue(result.score < 10.0)
    }

    @Test
    fun `fuzzy similarity catches typos but stays weak`() {
        val exact = rank("binary", node("1", "Binary Search"))!!.score
        val typo = rank("binry", node("1", "Binary Search"))!!.score

        assertTrue(typo > 0)
        assertTrue(typo < exact)
    }

    @Test
    fun `snippet prefers the personal template over the official syntax`() {
        val override = PrivateOverrideEntity(id = "ov", nodeId = "1", template = "MY TEMPLATE")
        val result = rank("for", node("1", "For Loop", syntax = "official()"), override)!!

        assertTrue(result.snippet!!.contains("MY TEMPLATE"))
    }

    @Test
    fun `unrelated content does not match`() {
        assertNull(rank("binary", node("1", "Cooking", markdown = "pasta recipe")))
    }
}
