package com.muddassir.deathcode.keyboard.input

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * [TextEditor] backed by the active [InputConnection].
 *
 * Cursor placement is implemented with a split commit rather than `setSelection`, because
 * the keyboard has no reliable way to know the caret's absolute index inside an arbitrary
 * editor. Committing the head with `newCursorPosition = 1` and then the tail with a
 * negative offset leaves the caret exactly at the intended offset and works in every editor
 * that implements the standard input APIs.
 */
class AndroidTextEditor(
    private val connectionProvider: () -> InputConnection?,
) : TextEditor {

    override fun textBeforeCursor(maxChars: Int): String =
        connectionProvider()?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()

    override fun deleteBackwards(count: Int) {
        if (count <= 0) return
        connectionProvider()?.deleteSurroundingText(count, 0)
    }

    override fun insertAtCursor(text: String, cursorOffsetFromStart: Int) {
        val connection = connectionProvider() ?: return
        val offset = cursorOffsetFromStart.coerceIn(0, text.length)
        val head = text.substring(0, offset)
        val tail = text.substring(offset)

        if (head.isNotEmpty()) {
            connection.commitText(head, 1)
        }
        if (tail.isNotEmpty()) {
            // Negative offset counts characters *before* the start of the committed text,
            // which puts the caret exactly between head and tail.
            connection.commitText(tail, -tail.length)
        }
    }

    override fun commit(text: String) {
        if (text.isEmpty()) return
        connectionProvider()?.commitText(text, 1)
    }

    override fun sendEnter() {
        val connection = connectionProvider() ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }
}
