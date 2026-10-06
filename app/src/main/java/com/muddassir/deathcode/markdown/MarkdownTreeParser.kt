package com.muddassir.deathcode.markdown

/**
 * A heading that is really a well-known content section rather than a category.
 *
 * Markdown is both the hierarchy format *and* the content format, so a document like:
 * ```markdown
 * # For Loop
 * The for loop repeats a block.
 * ## Syntax
 * ```cpp
 * for (int i = 0; i < n; i++) {}
 * ```
 * ## Keywords
 * - for
 * - forloop
 * ```
 * should produce a card titled "For Loop" with its [SectionKind.SYNTAX] and
 * [SectionKind.KEYWORDS] filled in, not two extra child categories.
 */
enum class SectionKind { NONE, SYNTAX, KEYWORDS, NOTES }

/** One node of the hierarchy produced from a markdown document. */
data class HierarchyNode(
    /** Heading text, e.g. `For Loop`. */
    val title: String,
    /** Depth relative to the document's own top level (1-based). */
    val level: Int,
    /** The raw markdown between this heading and the next same-or-higher heading. */
    val content: String,
    val sectionKind: SectionKind,
    val children: MutableList<HierarchyNode> = mutableListOf(),
) {
    val isLeaf: Boolean get() = children.isEmpty()
}

data class HierarchyParseResult(
    val roots: List<HierarchyNode>,
    val warnings: List<String>,
    val headingCount: Int,
) {
    val isEmpty: Boolean get() = roots.isEmpty()
}

/**
 * Parses markdown headings into a tree **relative to the document**, never relative to a
 * global depth. `#` is the first level of the imported document no matter where the import
 * happens, which is what makes markdown import work at unlimited nesting depth.
 *
 * Fenced code blocks are respected, so `#` inside a code sample is never a heading.
 */
object MarkdownTreeParser {

    private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.*?)\s*#*\s*$""")
    private val FENCE_REGEX = Regex("""^\s*(`{3,}|~{3,})(.*)$""")

    fun parse(markdown: String): HierarchyParseResult {
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val warnings = mutableListOf<String>()

        val headings = mutableListOf<RawHeading>()
        var inFence = false
        var fenceMarker = ""

        lines.forEachIndexed { index, line ->
            val fence = FENCE_REGEX.find(line)
            if (fence != null) {
                val marker = fence.groupValues[1]
                if (!inFence) {
                    inFence = true
                    fenceMarker = marker.first().toString()
                } else if (marker.startsWith(fenceMarker)) {
                    inFence = false
                    fenceMarker = ""
                }
                return@forEachIndexed
            }
            if (inFence) return@forEachIndexed

            val match = HEADING_REGEX.find(line) ?: return@forEachIndexed
            val level = match.groupValues[1].length
            val title = match.groupValues[2].trim()
            if (title.isEmpty()) {
                warnings += "Line ${index + 1}: heading has no title and was skipped."
                return@forEachIndexed
            }
            headings += RawHeading(level = level, title = title, lineIndex = index)
        }

        if (headings.isEmpty()) {
            return HierarchyParseResult(
                roots = emptyList(),
                warnings = warnings + "No markdown headings were found.",
                headingCount = 0,
            )
        }

        val baseLevel = headings.minOf { it.level }
        val roots = mutableListOf<HierarchyNode>()
        val stack = ArrayDeque<HierarchyNode>()
        var previousEffectiveLevel = 0

        headings.forEachIndexed { i, heading ->
            val nextLine = headings.getOrNull(i + 1)?.lineIndex ?: lines.size
            val content = lines.subList(heading.lineIndex + 1, nextLine)
                .joinToString("\n")
                .trim('\n')
                .trim()

            var effectiveLevel = heading.level - baseLevel + 1
            if (effectiveLevel > previousEffectiveLevel + 1) {
                warnings +=
                    "\"${heading.title}\" jumped ${effectiveLevel - previousEffectiveLevel} heading levels; " +
                    "it was placed at depth ${previousEffectiveLevel + 1}."
                effectiveLevel = previousEffectiveLevel + 1
            }
            previousEffectiveLevel = effectiveLevel

            val node = HierarchyNode(
                title = heading.title,
                level = effectiveLevel,
                content = content,
                sectionKind = detectSection(heading.title),
            )

            while (stack.isNotEmpty() && stack.last().level >= effectiveLevel) {
                stack.removeLast()
            }
            if (stack.isEmpty()) {
                roots += node
            } else {
                stack.last().children += node
            }
            stack.addLast(node)
        }

        return HierarchyParseResult(
            roots = roots,
            warnings = warnings,
            headingCount = headings.size,
        )
    }

    /** Detects well-known leaf sections that should populate a card field instead. */
    fun detectSection(title: String): SectionKind {
        val key = title.trim().lowercase().removeSuffix(":").trim()
        return when (key) {
            "keywords", "keyboard keywords", "keyword", "tags" -> SectionKind.KEYWORDS
            "syntax", "code", "signature", "template" -> SectionKind.SYNTAX
            "notes", "note", "remarks", "example", "examples" -> SectionKind.NOTES
            else -> SectionKind.NONE
        }
    }

    private data class RawHeading(val level: Int, val title: String, val lineIndex: Int)
}
