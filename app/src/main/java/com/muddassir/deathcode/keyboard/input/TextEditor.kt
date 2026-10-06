package com.muddassir.deathcode.keyboard.input

/**
 * The small slice of editor functionality the keyboard needs.
 *
 * Isolating it behind an interface keeps snippet insertion logic testable on the JVM and
 * means the keyboard only ever uses standard Android input APIs (never app-specific
 * injection), which is what lets it work across third-party editors.
 */
interface TextEditor {

    /** Text immediately before the caret, capped at [maxChars]. */
    fun textBeforeCursor(maxChars: Int): String

    /** Deletes [count] characters before the caret. */
    fun deleteBackwards(count: Int)

    /**
     * Inserts [text] at the caret and leaves the caret
     * [cursorOffsetFromStart] characters into the inserted text.
     */
    fun insertAtCursor(text: String, cursorOffsetFromStart: Int)

    /** Appends [text] at the caret. */
    fun commit(text: String)

    fun sendEnter()
}
