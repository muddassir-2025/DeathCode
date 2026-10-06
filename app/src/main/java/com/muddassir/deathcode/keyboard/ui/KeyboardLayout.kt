package com.muddassir.deathcode.keyboard.ui

/** A key on the programming symbol row, with the symbols offered on long press. */
data class SymbolKey(val primary: String, val alternatives: List<String> = emptyList())

/**
 * Layout data for the Death Code Keyboard.
 *
 * The QWERTY alphabet is always complete — programming symbols live in their own scrollable
 * row and on a secondary page, so letters are never sacrificed for punctuation.
 */
object KeyboardLayout {

    val letterRows: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm",
    )

    /**
     * Horizontally scrollable programming symbol row, ordered by how often the symbol is
     * used in code. Long press reveals related operators.
     */
    val symbolRow: List<SymbolKey> = listOf(
        SymbolKey("{"),
        SymbolKey("}", listOf("}", "{")),
        SymbolKey("(", listOf("(", ")", "[", "]", "{", "}")),
        SymbolKey(")", listOf(")", "(")),
        SymbolKey("[", listOf("[", "]")),
        SymbolKey("]", listOf("]", "[")),
        SymbolKey("<", listOf("<", ">", "<=", ">=", "<<", ">>")),
        SymbolKey(">", listOf(">", "<", ">=", "<=")),
        SymbolKey(";", listOf(";")),
        SymbolKey(":", listOf(":", "::", ":=")),
        SymbolKey("=", listOf("=", "==", "!=", "<=", ">=", "=>")),
        SymbolKey("-", listOf("-", "->", "--", "-=")),
        SymbolKey("+", listOf("+", "++", "+=")),
        SymbolKey("*", listOf("*", "**", "*=")),
        SymbolKey("/", listOf("/", "//", "/*", "*/", "/=")),
        SymbolKey("%", listOf("%", "%=")),
        SymbolKey("!", listOf("!", "!=", "!!")),
        SymbolKey("&", listOf("&", "&&", "&=")),
        SymbolKey("|", listOf("|", "||", "|=")),
        SymbolKey("^", listOf("^", "^=")),
        SymbolKey("~", listOf("~")),
        SymbolKey(".", listOf(".", "->", "...", "::")),
        SymbolKey(",", listOf(",", ";")),
        SymbolKey("_", listOf("_")),
        SymbolKey("#", listOf("#", "##")),
        SymbolKey("@", listOf("@")),
        SymbolKey("\$", listOf("\$", "\${", "}")),
        SymbolKey("?", listOf("?", "?.")),
        SymbolKey("'", listOf("'", "\"")),
        SymbolKey("\"", listOf("\"", "'")),
    )

    /** Secondary page shown when the number/symbol key is pressed. */
    val symbolPageRows: List<List<SymbolKey>> = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { SymbolKey(it) },
        listOf(
            SymbolKey("-", listOf("-", "_", "->")),
            SymbolKey("/", listOf("/", "\\")),
            SymbolKey(":", listOf(":", ";")),
            SymbolKey(";", listOf(";", ":")),
            SymbolKey("(", listOf("(", ")")),
            SymbolKey(")", listOf(")", "(")),
            SymbolKey("\$", listOf("\$", "€", "£")),
            SymbolKey("&", listOf("&", "&&")),
            SymbolKey("@", listOf("@", "#")),
            SymbolKey("\"", listOf("\"", "'")),
        ),
        listOf(
            SymbolKey("."),
            SymbolKey(",", listOf(",", ";")),
            SymbolKey("?", listOf("?", "!")),
            SymbolKey("!", listOf("!", "?")),
            SymbolKey("'"),
            SymbolKey("+", listOf("+", "=")),
            SymbolKey("=", listOf("=", "==", "!=")),
            SymbolKey("*", listOf("*", "**")),
            SymbolKey("%", listOf("%", "%=")),
            SymbolKey("~", listOf("~", "^")),
        ),
    )
}
