package com.muddassir.deathcode.keyboard

import com.muddassir.deathcode.keyboard.input.TextEditor
import com.muddassir.deathcode.keyboard.snippets.TemplateInserter
import org.junit.Assert.assertEquals
import org.junit.Test

/** In-memory editor used to verify insertion behaviour without Android. */
private class FakeEditor(private var text: String = "", private var cursor: Int = text.length) : TextEditor {

    override fun textBeforeCursor(maxChars: Int): String =
        text.substring(0, cursor).takeLast(maxChars)

    override fun deleteBackwards(count: Int) {
        val from = (cursor - count).coerceAtLeast(0)
        text = text.removeRange(from, cursor)
        cursor = from
    }

    override fun insertAtCursor(inserted: String, cursorOffsetFromStart: Int) {
        text = text.substring(0, cursor) + inserted + text.substring(cursor)
        cursor += cursorOffsetFromStart
    }

    override fun commit(committed: String) = insertAtCursor(committed, committed.length)

    override fun sendEnter() = commit("\n")

    fun currentText(): String = text
    fun cursorPosition(): Int = cursor
}

class TemplateInserterTest {

    @Test
    fun `extracts the trailing word`() {
        assertEquals("for", TemplateInserter.trailingWord("int x = 1; for"))
        assertEquals("bfs", TemplateInserter.trailingWord("bfs"))
        assertEquals("", TemplateInserter.trailingWord("for "))
    }

    @Test
    fun `replaces the typed keyword with the template`() {
        val editor = FakeEditor("for")

        TemplateInserter.insert(editor, "for", "for (int i = 0; i < n; i++) {\n    \${cursor}\n}")

        assertEquals("for (int i = 0; i < n; i++) {\n    \n}", editor.currentText())
        assertEquals(editor.currentText().indexOf("\n}"), editor.cursorPosition())
    }

    @Test
    fun `keeps surrounding text intact`() {
        val editor = FakeEditor("int main() {\n  tri")

        TemplateInserter.insert(editor, "tri", "for (int i = 1; i <= n; i++) {}\n\${cursor}")

        assertEquals("int main() {\n  for (int i = 1; i <= n; i++) {}\n", editor.currentText())
        assertEquals(editor.currentText().length, editor.cursorPosition())
    }

    @Test
    fun `inserts at the caret when there is no keyword to replace`() {
        val editor = FakeEditor("")

        TemplateInserter.insert(editor, "", "cout << \${cursor};")

        assertEquals("cout << ;", editor.currentText())
        assertEquals("cout << ".length, editor.cursorPosition())
    }
}
