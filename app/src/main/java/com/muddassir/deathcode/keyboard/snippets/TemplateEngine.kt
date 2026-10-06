package com.muddassir.deathcode.keyboard.snippets

/** Where a named placeholder ended up in the expanded text. */
data class PlaceholderRange(val name: String, val start: Int, val end: Int)

/**
 * The result of expanding a snippet template.
 *
 * [text] is what should be committed to the editor. [cursorOffset] is where the caret
 * should end up. [placeholders] lets the keyboard select-and-replace named placeholders.
 */
data class ExpandedTemplate(
    val text: String,
    val cursorOffset: Int,
    val placeholders: List<PlaceholderRange> = emptyList(),
)

/**
 * Expands snippet templates that contain placeholders.
 *
 * Supported syntax:
 * ```
 * for (${initializer}; ${condition}; ${increment}) {
 *     ${cursor}
 * }
 * ```
 *
 * - `${cursor}` / `${0}` marks the caret and contributes no text.
 * - `${name}` is replaced by the literal `name` and recorded in
 *   [ExpandedTemplate.placeholders] so the keyboard can preselect it.
 * - `$1`-style numeric placeholders are treated like named ones.
 *
 * The engine is pure and unit tested; the IME only has to apply the result through the
 * Android [android.view.inputmethod.InputConnection].
 */
object TemplateEngine {

    private val PLACEHOLDER_REGEX = Regex("""\$\{([A-Za-z0-9_]*)\}|\$(\d)""")

    const val CURSOR_TOKEN = "cursor"

    fun expand(template: String): ExpandedTemplate {
        val sb = StringBuilder(template.length)
        val placeholders = mutableListOf<PlaceholderRange>()
        var cursor: Int? = null

        var lastIndex = 0
        for (match in PLACEHOLDER_REGEX.findAll(template)) {
            sb.append(template, lastIndex, match.range.first)
            val name = (match.groupValues[1].ifEmpty { match.groupValues[2] }).trim()

            when {
                name.isEmpty() || name == CURSOR_TOKEN || name == "0" -> {
                    if (cursor == null) cursor = sb.length
                }
                else -> {
                    val start = sb.length
                    sb.append(name)
                    placeholders += PlaceholderRange(name, start, sb.length)
                }
            }
            lastIndex = match.range.last + 1
        }
        sb.append(template, lastIndex, template.length)

        val resolvedCursor = cursor ?: placeholders.firstOrNull()?.start ?: sb.length
        return ExpandedTemplate(
            text = sb.toString(),
            cursorOffset = resolvedCursor,
            placeholders = placeholders,
        )
    }

    /** True when the template contains any placeholder the engine understands. */
    fun hasPlaceholders(template: String): Boolean = PLACEHOLDER_REGEX.containsMatchIn(template)

    /** A compact preview for the suggestion strip (first meaningful line). */
    fun preview(template: String, maxLength: Int = 42): String {
        val expanded = expand(template).text
        val line = expanded.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return if (line.length <= maxLength) line else line.take(maxLength - 1) + "…"
    }
}
