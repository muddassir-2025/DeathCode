package com.muddassir.deathcode.markdown

/** A renderable markdown block. */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock
    data class ListBlock(val ordered: Boolean, val start: Int, val items: List<ListItem>) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

data class ListItem(val text: String, val depth: Int)

/** A renderable inline markdown span. */
sealed interface InlineSpan {
    data class Plain(val text: String) : InlineSpan
    data class Bold(val text: String) : InlineSpan
    data class Italic(val text: String) : InlineSpan
    data class Code(val text: String) : InlineSpan
    data class Link(val text: String, val url: String) : InlineSpan
}

/**
 * Line based markdown parser producing a flat list of blocks for the reader UI.
 *
 * Deliberately small and dependency free: the product must render programming
 * documentation offline and deterministically, without pulling in a web view or an
 * external markdown engine.
 */
object MarkdownBlockParser {

    private val FENCE_REGEX = Regex("""^\s*(`{3,}|~{3,})\s*([\w+#.-]*)\s*$""")
    private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.*?)\s*#*\s*$""")
    private val BULLET_REGEX = Regex("""^(\s*)[-*+]\s+(.*)$""")
    private val ORDERED_REGEX = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")
    private val QUOTE_REGEX = Regex("""^\s*>\s?(.*)$""")
    private val DIVIDER_REGEX = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")

    fun parse(markdown: String): List<MarkdownBlock> {
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = mutableListOf<String>()
        var index = 0

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n").trim())
                paragraph.clear()
            }
        }

        while (index < lines.size) {
            val line = lines[index]

            val fence = FENCE_REGEX.find(line)
            if (fence != null) {
                flushParagraph()
                val marker = fence.groupValues[1].first()
                val language = fence.groupValues[2].ifBlank { null }
                val code = StringBuilder()
                index++
                while (index < lines.size) {
                    val candidate = lines[index]
                    val closing = FENCE_REGEX.find(candidate)
                    if (closing != null &&
                        closing.groupValues[1].first() == marker &&
                        closing.groupValues[2].isBlank()
                    ) {
                        break
                    }
                    code.append(candidate).append('\n')
                    index++
                }
                blocks += MarkdownBlock.CodeBlock(language, code.toString().trimEnd('\n'))
                index++
                continue
            }

            if (line.isBlank()) {
                flushParagraph()
                index++
                continue
            }

            val heading = HEADING_REGEX.find(line)
            if (heading != null) {
                flushParagraph()
                blocks += MarkdownBlock.Heading(
                    heading.groupValues[1].length,
                    heading.groupValues[2].trim(),
                )
                index++
                continue
            }

            if (DIVIDER_REGEX.matches(line)) {
                flushParagraph()
                blocks += MarkdownBlock.Divider
                index++
                continue
            }

            if (BULLET_REGEX.matches(line) || ORDERED_REGEX.matches(line)) {
                flushParagraph()
                val ordered = ORDERED_REGEX.matches(line) && !BULLET_REGEX.matches(line)
                val items = mutableListOf<ListItem>()
                var start = 1
                while (index < lines.size) {
                    val current = lines[index]
                    val order = ORDERED_REGEX.find(current)
                    val bullet = BULLET_REGEX.find(current)
                    if (ordered && order == null) break
                    if (!ordered && bullet == null) break
                    if (ordered) {
                        val indent = order!!.groupValues[1].length
                        val text = order.groupValues[3]
                        if (items.isEmpty()) start = order.groupValues[2].toIntOrNull() ?: 1
                        items += ListItem(text.trim(), indent / 2)
                    } else {
                        val indent = bullet!!.groupValues[1].length
                        items += ListItem(bullet.groupValues[2].trim(), indent / 2)
                    }
                    index++
                }
                if (items.isNotEmpty()) blocks += MarkdownBlock.ListBlock(ordered, start, items)
                continue
            }

            val quote = QUOTE_REGEX.find(line)
            if (quote != null) {
                flushParagraph()
                val collected = mutableListOf(quote.groupValues[1])
                index++
                while (index < lines.size) {
                    val next = QUOTE_REGEX.find(lines[index]) ?: break
                    collected += next.groupValues[1]
                    index++
                }
                blocks += MarkdownBlock.Quote(collected.joinToString("\n").trim())
                continue
            }

            if (line.contains('|') && index + 1 < lines.size && isTableSeparator(lines[index + 1])) {
                flushParagraph()
                val headers = splitRow(line)
                index += 2
                val rows = mutableListOf<List<String>>()
                while (index < lines.size && lines[index].isNotBlank() && lines[index].contains('|')) {
                    rows += splitRow(lines[index])
                    index++
                }
                blocks += MarkdownBlock.Table(headers, rows)
                continue
            }

            paragraph += line
            index++
        }

        flushParagraph()
        return blocks
    }

    private fun isTableSeparator(line: String): Boolean =
        line.contains('|') &&
            line.count { it == '|' } >= 2 &&
            line.trim().trim('|').split('|').all { cell ->
                val trimmed = cell.trim()
                trimmed.isNotEmpty() && trimmed.all { it == '-' || it == ':' || it == ' ' }
            }

    private fun splitRow(line: String): List<String> =
        line.trim().trim('|').split('|').map { it.trim() }

    /** Parses inline emphasis / code / links. */
    fun parseInline(text: String): List<InlineSpan> {
        val spans = mutableListOf<InlineSpan>()
        val plain = StringBuilder()
        var i = 0

        fun flush() {
            if (plain.isNotEmpty()) {
                spans += InlineSpan.Plain(plain.toString())
                plain.clear()
            }
        }

        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        flush()
                        spans += InlineSpan.Code(text.substring(i + 1, end))
                        i = end + 1
                    } else {
                        plain.append(ch)
                        i++
                    }
                }

                ch == '*' && i + 1 < text.length && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        flush()
                        spans += InlineSpan.Bold(text.substring(i + 2, end))
                        i = end + 2
                    } else {
                        plain.append(ch)
                        i++
                    }
                }

                ch == '*' || ch == '_' -> {
                    val end = text.indexOf(ch, i + 1)
                    if (end > i + 1) {
                        flush()
                        spans += InlineSpan.Italic(text.substring(i + 1, end))
                        i = end + 1
                    } else {
                        plain.append(ch)
                        i++
                    }
                }

                ch == '[' -> {
                    val closeLabel = text.indexOf(']', i + 1)
                    val openUrl = if (closeLabel >= 0) text.indexOf('(', closeLabel) else -1
                    val closeUrl = if (openUrl >= 0) text.indexOf(')', openUrl) else -1
                    if (closeLabel > i && openUrl == closeLabel + 1 && closeUrl > openUrl) {
                        flush()
                        spans += InlineSpan.Link(
                            text = text.substring(i + 1, closeLabel),
                            url = text.substring(openUrl + 1, closeUrl),
                        )
                        i = closeUrl + 1
                    } else {
                        plain.append(ch)
                        i++
                    }
                }

                else -> {
                    plain.append(ch)
                    i++
                }
            }
        }
        flush()
        return spans
    }
}
