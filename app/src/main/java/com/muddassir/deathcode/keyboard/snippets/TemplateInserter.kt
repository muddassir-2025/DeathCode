package com.muddassir.deathcode.keyboard.snippets

import com.muddassir.deathcode.keyboard.input.TextEditor

/**
 * Replaces the keyword the user is currently typing with an expanded snippet template.
 *
 * Kept free of Android dependencies so the cursor/placeholder behaviour is unit testable
 * with a fake [TextEditor].
 */
object TemplateInserter {

    private const val WORD_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"

    /** The trailing word at the end of [text], i.e. the keyword being typed. */
    fun trailingWord(text: String): String {
        var index = text.length
        while (index > 0 && text[index - 1] in WORD_CHARS) {
            index--
        }
        return text.substring(index)
    }

    /**
     * Deletes [word], inserts the expanded [template] and places the caret.
     *
     * When [word] is empty the template is simply inserted at the caret.
     */
    fun insert(editor: TextEditor, word: String, template: String): ExpandedTemplate {
        val expanded = TemplateEngine.expand(template)
        if (word.isNotEmpty()) {
            editor.deleteBackwards(word.length)
        }
        editor.insertAtCursor(expanded.text, expanded.cursorOffset)
        return expanded
    }
}
