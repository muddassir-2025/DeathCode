package com.muddassir.deathcode.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTreeParserTest {

    @Test
    fun `parses headings relative to the document, not globally`() {
        val markdown = """
            ## Arrays

            ### Two Pointer

            ### Sliding Window
        """.trimIndent()

        val result = MarkdownTreeParser.parse(markdown)

        assertEquals(1, result.roots.size)
        assertEquals("Arrays", result.roots[0].title)
        assertEquals(1, result.roots[0].level)
        assertEquals(2, result.roots[0].children.size)
        assertEquals("Two Pointer", result.roots[0].children[0].title)
        assertEquals("Sliding Window", result.roots[0].children[1].title)
        assertEquals(2, result.roots[0].children[0].level)
    }

    @Test
    fun `the same document produces the same tree at any import depth`() {
        val document = "# A\n## B\n### C"

        val shallow = MarkdownTreeParser.parse(document)
        val deeper = MarkdownTreeParser.parse(document)

        assertEquals("A", shallow.roots.single().title)
        assertEquals("A", deeper.roots.single().title)
        assertEquals("C", shallow.roots.single().children.single().children.single().title)
        assertEquals("C", deeper.roots.single().children.single().children.single().title)
    }

    @Test
    fun `ignores headings inside fenced code blocks`() {
        val markdown = """
            # Loops

            ```python
            # this is a comment, not a heading
            print("hi")
            ```

            ## While Loop
        """.trimIndent()

        val result = MarkdownTreeParser.parse(markdown)

        assertEquals(1, result.roots.size)
        assertEquals("Loops", result.roots[0].title)
        assertEquals(1, result.roots[0].children.size)
        assertEquals("While Loop", result.roots[0].children[0].title)
        assertTrue(result.roots[0].content.contains("# this is a comment"))
    }

    @Test
    fun `normalizes skipped heading levels and warns`() {
        val markdown = "# A\n### Deep"

        val result = MarkdownTreeParser.parse(markdown)

        assertEquals(1, result.roots.single().children.size)
        assertEquals("Deep", result.roots.single().children.single().title)
        assertEquals(2, result.roots.single().children.single().level)
        assertTrue(result.warnings.any { it.contains("jumped") })
    }

    @Test
    fun `reports an empty document when there are no headings`() {
        val result = MarkdownTreeParser.parse("just a paragraph\nwith no heading")

        assertTrue(result.isEmpty)
        assertTrue(result.warnings.any { it.contains("No markdown headings") })
    }

    @Test
    fun `captures the content between headings`() {
        val markdown = """
            # For Loop

            Repeats a block.

            ## Syntax

            ```cpp
            for (;;) {}
            ```
        """.trimIndent()

        val result = MarkdownTreeParser.parse(markdown)
        val root = result.roots.single()

        assertEquals("Repeats a block.", root.content)
        assertEquals(SectionKind.SYNTAX, root.children.single().sectionKind)
    }

    @Test
    fun `detects well known leaf sections`() {
        assertEquals(SectionKind.KEYWORDS, MarkdownTreeParser.detectSection("Keywords"))
        assertEquals(SectionKind.KEYWORDS, MarkdownTreeParser.detectSection("Keyboard Keywords:"))
        assertEquals(SectionKind.SYNTAX, MarkdownTreeParser.detectSection("Syntax"))
        assertEquals(SectionKind.NOTES, MarkdownTreeParser.detectSection("Notes"))
        assertEquals(SectionKind.NONE, MarkdownTreeParser.detectSection("Fundamentals"))
    }

    @Test
    fun `supports unlimited nesting depth`() {
        val markdown = "# 1\n## 2\n### 3\n#### 4\n##### 5\n###### 6"

        val result = MarkdownTreeParser.parse(markdown)

        var node: HierarchyNode? = result.roots.single()
        val titles = mutableListOf<String>()
        while (node != null) {
            titles += node.title
            node = node.children.firstOrNull()
        }
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), titles)
    }
}
