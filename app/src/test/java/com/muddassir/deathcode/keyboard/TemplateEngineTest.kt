package com.muddassir.deathcode.keyboard

import com.muddassir.deathcode.keyboard.snippets.TemplateEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateEngineTest {

    @Test
    fun `cursor placeholder is removed and positions the caret`() {
        val template = "for (int i = 0; i < n; i++) {\n    \${cursor}\n}"
        val expanded = TemplateEngine.expand(template)

        assertFalse(expanded.text.contains("cursor"))
        assertEquals(
            expanded.text.indexOf("\n}"),
            expanded.cursorOffset,
        )
    }

    @Test
    fun `named placeholders become selectable text`() {
        val expanded = TemplateEngine.expand("while (\${condition}) {\n    \${cursor}\n}")

        assertTrue(expanded.text.contains("condition"))
        assertEquals(1, expanded.placeholders.size)
        assertEquals("condition", expanded.placeholders.single().name)
        val range = expanded.placeholders.single()
        assertEquals("condition", expanded.text.substring(range.start, range.end))
    }

    @Test
    fun `cursor defaults to the first placeholder when none is declared`() {
        val expanded = TemplateEngine.expand("sort(\${vector});")

        assertEquals(expanded.placeholders.first().start, expanded.cursorOffset)
    }

    @Test
    fun `cursor defaults to the end when the template has no placeholders`() {
        val expanded = TemplateEngine.expand("ios::sync_with_stdio(false);")

        assertEquals(expanded.text.length, expanded.cursorOffset)
        assertFalse(TemplateEngine.hasPlaceholders("ios::sync_with_stdio(false);"))
    }

    @Test
    fun `template without any placeholder is inserted unchanged`() {
        val template = "vector<int> v(n, 0);"
        assertEquals(template, TemplateEngine.expand(template).text)
    }

    @Test
    fun `preview uses the first meaningful line`() {
        val preview = TemplateEngine.preview("for (int i = 0; i < n; i++) {\n    \${cursor}\n}")

        assertEquals("for (int i = 0; i < n; i++) {", preview)
    }
}
