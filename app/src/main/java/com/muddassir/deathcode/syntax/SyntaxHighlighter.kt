package com.muddassir.deathcode.syntax

/** A lexical category the UI maps to a colour. */
enum class TokenKind { PLAIN, KEYWORD, TYPE, STRING, COMMENT, NUMBER, FUNCTION, ANNOTATION }

data class CodeToken(val text: String, val kind: TokenKind)

/**
 * A small, deterministic lexer used for offline syntax highlighting.
 *
 * It intentionally does not attempt full language parsing — it recognizes comments,
 * strings, numbers, keywords, types and call-like identifiers, which is what makes code
 * readable on a phone. Extending a language means extending [Languages].
 */
object SyntaxHighlighter {

    fun tokenize(code: String, language: String?): List<CodeToken> {
        val spec = Languages.byId(language)
        val tokens = mutableListOf<CodeToken>()
        var i = 0

        fun peek(offset: Int = 0): Char? = code.getOrNull(i + offset)

        fun startsWith(value: String): Boolean = code.startsWith(value, i, ignoreCase = !(spec?.caseSensitive ?: true))

        fun emit(text: String, kind: TokenKind) {
            tokens += CodeToken(text, kind)
        }

        while (i < code.length) {
            val ch = code[i]

            // --- block comment ---
            val block = spec?.blockComment
            if (block != null && startsWith(block.first)) {
                val end = code.indexOf(block.second, i + block.first.length)
                val stop = if (end < 0) code.length else end + block.second.length
                emit(code.substring(i, stop), TokenKind.COMMENT)
                i = stop
                continue
            }

            // --- line comment ---
            val lineComment = when {
                spec?.lineComment != null && startsWith(spec.lineComment) -> spec.lineComment
                spec?.hashComment == true && ch == '#' -> "#"
                else -> null
            }
            if (lineComment != null) {
                var end = code.indexOf('\n', i)
                if (end < 0) end = code.length
                emit(code.substring(i, end), TokenKind.COMMENT)
                i = end
                continue
            }

            // --- string literal ---
            val delimiters = spec?.stringDelimiters ?: listOf('"', '\'')
            if (ch in delimiters) {
                val delimiter = ch
                val sb = StringBuilder().append(ch)
                var j = i + 1
                var terminated = false
                while (j < code.length) {
                    val c = code[j]
                    sb.append(c)
                    if (c == '\\' && j + 1 < code.length) {
                        sb.append(code[j + 1])
                        j += 2
                        continue
                    }
                    if (c == delimiter) {
                        terminated = true
                        j++
                        break
                    }
                    j++
                }
                emit(sb.toString(), TokenKind.STRING)
                i = if (terminated) j else code.length
                continue
            }

            // --- number ---
            if (ch.isDigit() && !isIdentifierChar(peek(-1))) {
                val sb = StringBuilder()
                var j = i
                while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) {
                    sb.append(code[j]); j++
                }
                emit(sb.toString(), TokenKind.NUMBER)
                i = j
                continue
            }

            // --- annotation / decorator ---
            if ((ch == '@' || ch == '#') && spec != null) {
                val sb = StringBuilder()
                var j = i
                while (j < code.length && isIdentifierChar(code[j])) {
                    sb.append(code[j]); j++
                }
                if (sb.length > 1) {
                    emit(sb.toString(), TokenKind.ANNOTATION)
                    i = j
                    continue
                }
            }

            // --- identifier / keyword / type / function ---
            if (ch.isLetter() || ch == '_' || ch == '$') {
                val sb = StringBuilder()
                var j = i
                while (j < code.length && isIdentifierChar(code[j])) {
                    sb.append(code[j]); j++
                }
                val word = sb.toString()
                val lookup = if (spec?.caseSensitive == false) word.lowercase() else word
                val kind = when {
                    spec == null -> TokenKind.PLAIN
                    lookup in spec.keywords -> TokenKind.KEYWORD
                    lookup in spec.types -> TokenKind.TYPE
                    word.first().isUpperCase() && (spec.id == "cpp" || spec.id == "java" ||
                        spec.id == "kotlin" || spec.id == "swift" || spec.id == "c") -> TokenKind.TYPE
                    isCallSite(code, j) -> TokenKind.FUNCTION
                    else -> TokenKind.PLAIN
                }
                emit(word, kind)
                i = j
                continue
            }

            emit(ch.toString(), TokenKind.PLAIN)
            i++
        }

        return tokens
    }

    private fun isIdentifierChar(c: Char?): Boolean =
        c != null && (c.isLetterOrDigit() || c == '_' || c == '$')

    private fun isCallSite(code: String, indexAfterWord: Int): Boolean {
        var j = indexAfterWord
        while (j < code.length && code[j] == ' ') j++
        return j < code.length && code[j] == '('
    }
}
