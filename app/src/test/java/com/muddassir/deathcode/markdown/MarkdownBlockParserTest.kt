package com.muddassir.deathcode.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownBlockParserTest {

    @Test
    fun `parses fenced code with a language`() {
        val blocks = MarkdownBlockParser.parse("text\n\n```cpp\nfor (int i = 0; i < n; i++) {}\n```\n")

        val code = blocks.filterIsInstance<MarkdownBlock.CodeBlock>().single()
        assertEquals("cpp", code.language)
        assertEquals("for (int i = 0; i < n; i++) {}", code.code)
    }

    @Test
    fun `does not treat a heading-like line inside code as a heading`() {
        val blocks = MarkdownBlockParser.parse("```\n# not a heading\n```")

        assertTrue(blocks.none { it is MarkdownBlock.Heading })
    }

    @Test
    fun `parses unordered and ordered lists`() {
        val bullets = MarkdownBlockParser.parse("- for\n- while\n")
        val list = bullets.filterIsInstance<MarkdownBlock.ListBlock>().single()
        assertEquals(false, list.ordered)
        assertEquals(listOf("for", "while"), list.items.map { it.text })

        val ordered = MarkdownBlockParser.parse("1. first\n2. second\n")
        val orderedList = ordered.filterIsInstance<MarkdownBlock.ListBlock>().single()
        assertEquals(true, orderedList.ordered)
        assertEquals(1, orderedList.start)
    }

    @Test
    fun `parses blockquotes and dividers`() {
        val blocks = MarkdownBlockParser.parse("> note here\n\n---\n")
        assertEquals("note here", blocks.filterIsInstance<MarkdownBlock.Quote>().single().text)
        assertTrue(blocks.any { it is MarkdownBlock.Divider })
    }

    @Test
    fun `parses tables`() {
        val md = "| a | b |\n| --- | --- |\n| 1 | 2 |\n"
        val table = MarkdownBlockParser.parse(md).filterIsInstance<MarkdownBlock.Table>().single()
        assertEquals(listOf("a", "b"), table.headers)
        assertEquals(listOf("1", "2"), table.rows.single())
    }

    @Test
    fun `parses inline spans`() {
        val spans = MarkdownBlockParser.parseInline("Use **bold**, *italic*, `code` and [link](http://x.dev)")

        assertTrue(spans.any { it is InlineSpan.Bold && it.text == "bold" })
        assertTrue(spans.any { it is InlineSpan.Italic && it.text == "italic" })
        assertTrue(spans.any { it is InlineSpan.Code && it.text == "code" })
        assertTrue(spans.any { it is InlineSpan.Link && it.url == "http://x.dev" })
    }

    @Test
    fun `leaves unmatched markers as plain text`() {
        val spans = MarkdownBlockParser.parseInline("a * b ` c")

        val plain = spans.filterIsInstance<InlineSpan.Plain>().joinToString("") { it.text }
        assertEquals("a * b ` c", plain)
    }
}
