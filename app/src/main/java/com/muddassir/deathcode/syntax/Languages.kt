package com.muddassir.deathcode.syntax

/**
 * Everything the highlighter and the editor need to know about one programming language.
 *
 * Adding a language is a one-line change in [Languages.all] — the highlighter, the language
 * pickers and the keyboard's language selector all read from this registry.
 */
data class LanguageSpec(
    val id: String,
    val displayName: String,
    val fileExtension: String,
    val keywords: Set<String>,
    val types: Set<String> = emptySet(),
    val lineComment: String? = "//",
    val blockComment: Pair<String, String>? = "/*" to "*/",
    val hashComment: Boolean = false,
    val stringDelimiters: List<Char> = listOf('"', '\''),
    val caseSensitive: Boolean = true,
)

object Languages {

    val cpp = LanguageSpec(
        id = "cpp",
        displayName = "C++",
        fileExtension = "cpp",
        keywords = setOf(
            "alignas", "alignof", "and", "asm", "auto", "break", "case", "catch", "class",
            "const", "consteval", "constexpr", "constinit", "const_cast", "continue",
            "co_await", "co_return", "co_yield", "decltype", "default", "delete", "do",
            "dynamic_cast", "else", "enum", "explicit", "export", "extern", "false",
            "for", "friend", "goto", "if", "inline", "mutable", "namespace", "new",
            "noexcept", "not", "nullptr", "operator", "or", "private", "protected",
            "public", "register", "reinterpret_cast", "requires", "return", "sizeof",
            "static", "static_assert", "static_cast", "struct", "switch", "template",
            "this", "thread_local", "throw", "true", "try", "typedef", "typeid",
            "typename", "union", "using", "virtual", "volatile", "while", "concept",
            "override", "final", "xor", "bitand", "bitor", "compl", "not_eq",
        ),
        types = setOf(
            "bool", "char", "char8_t", "char16_t", "char32_t", "double", "float", "int",
            "long", "short", "signed", "unsigned", "void", "wchar_t", "size_t", "string",
            "vector", "map", "set", "unordered_map", "unordered_set", "pair", "queue",
            "stack", "deque", "array", "list", "priority_queue", "tuple", "optional",
            "variant", "shared_ptr", "unique_ptr", "weak_ptr", "istream", "ostream",
        ),
    )

    val c = LanguageSpec(
        id = "c",
        displayName = "C",
        fileExtension = "c",
        keywords = setOf(
            "auto", "break", "case", "const", "continue", "default", "do", "else", "enum",
            "extern", "for", "goto", "if", "inline", "register", "restrict", "return",
            "sizeof", "static", "struct", "switch", "typedef", "union", "volatile", "while",
            "_Bool", "_Complex", "_Generic", "_Static_assert", "_Thread_local",
        ),
        types = setOf(
            "char", "double", "float", "int", "long", "short", "signed", "unsigned", "void",
            "size_t", "FILE", "int8_t", "int16_t", "int32_t", "int64_t", "uint8_t",
            "uint16_t", "uint32_t", "uint64_t", "bool",
        ),
    )

    val java = LanguageSpec(
        id = "java",
        displayName = "Java",
        fileExtension = "java",
        keywords = setOf(
            "abstract", "assert", "break", "case", "catch", "class", "const", "continue",
            "default", "do", "else", "enum", "extends", "final", "finally", "for", "goto",
            "if", "implements", "import", "instanceof", "interface", "native", "new",
            "package", "private", "protected", "public", "return", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "volatile", "while", "true", "false", "null", "var", "record", "sealed",
            "yield",
        ),
        types = setOf(
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void",
            "String", "Object", "Integer", "Double", "Long", "Boolean", "List", "Map",
            "Set", "ArrayList", "HashMap", "HashSet", "Optional", "Stream",
        ),
    )

    val python = LanguageSpec(
        id = "python",
        displayName = "Python",
        fileExtension = "py",
        keywords = setOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
            "del", "elif", "else", "except", "finally", "for", "from", "global", "if",
            "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise",
            "return", "try", "while", "with", "yield", "True", "False", "None", "match",
            "case", "self",
        ),
        types = setOf(
            "int", "float", "str", "bool", "bytes", "list", "dict", "set", "tuple",
            "frozenset", "complex", "object", "type", "Exception", "Any",
        ),
        hashComment = true,
        lineComment = null,
        blockComment = null,
        stringDelimiters = listOf('"', '\''),
    )

    val javascript = LanguageSpec(
        id = "javascript",
        displayName = "JavaScript",
        fileExtension = "js",
        keywords = setOf(
            "async", "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "export", "extends", "finally",
            "for", "function", "if", "import", "in", "instanceof", "let", "new", "of",
            "return", "static", "super", "switch", "this", "throw", "try", "typeof", "var",
            "void", "while", "with", "yield", "true", "false", "null", "undefined",
        ),
        types = setOf(
            "Array", "Object", "String", "Number", "Boolean", "Promise", "Map", "Set",
            "JSON", "Date", "RegExp", "Symbol", "BigInt", "Error",
        ),
    )

    val typescript = LanguageSpec(
        id = "typescript",
        displayName = "TypeScript",
        fileExtension = "ts",
        keywords = javascript.keywords + setOf(
            "interface", "type", "enum", "namespace", "declare", "readonly", "abstract",
            "implements", "private", "protected", "public", "as", "satisfies", "keyof",
            "infer", "is", "asserts", "override", "accessor", "is",
        ),
        types = javascript.types + setOf(
            "string", "number", "boolean", "any", "unknown", "never", "void", "object",
            "Record", "Partial", "Readonly", "Pick", "Omit",
        ),
    )

    val go = LanguageSpec(
        id = "go",
        displayName = "Go",
        fileExtension = "go",
        keywords = setOf(
            "break", "case", "chan", "const", "continue", "default", "defer", "else",
            "fallthrough", "for", "func", "go", "goto", "if", "import", "interface", "map",
            "package", "range", "return", "select", "struct", "switch", "type", "var",
            "nil", "true", "false", "iota",
        ),
        types = setOf(
            "bool", "byte", "complex64", "complex128", "error", "float32", "float64",
            "int", "int8", "int16", "int32", "int64", "rune", "string", "uint", "uint8",
            "uint16", "uint32", "uint64", "uintptr", "any",
        ),
    )

    val rust = LanguageSpec(
        id = "rust",
        displayName = "Rust",
        fileExtension = "rs",
        keywords = setOf(
            "as", "async", "await", "break", "const", "continue", "crate", "dyn", "else",
            "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop",
            "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self",
            "static", "struct", "super", "trait", "true", "type", "unsafe", "use", "where",
            "while", "union",
        ),
        types = setOf(
            "bool", "char", "f32", "f64", "i8", "i16", "i32", "i64", "i128", "isize",
            "str", "u8", "u16", "u32", "u64", "u128", "usize", "String", "Vec", "Option",
            "Result", "Box", "Rc", "Arc", "HashMap", "HashSet",
        ),
    )

    val kotlin = LanguageSpec(
        id = "kotlin",
        displayName = "Kotlin",
        fileExtension = "kt",
        keywords = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
            "in", "interface", "is", "null", "object", "package", "return", "super", "this",
            "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while",
            "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally",
            "get", "import", "init", "param", "property", "receiver", "set", "setparam",
            "where", "actual", "abstract", "annotation", "companion", "const", "crossinline",
            "data", "enum", "expect", "external", "final", "infix", "inline", "inner",
            "internal", "lateinit", "noinline", "open", "operator", "out", "override",
            "private", "protected", "public", "reified", "sealed", "suspend", "tailrec",
            "vararg", "it",
        ),
        types = setOf(
            "Boolean", "Byte", "Char", "Double", "Float", "Int", "Long", "Short", "String",
            "Unit", "Any", "Nothing", "List", "MutableList", "Map", "MutableMap", "Set",
            "MutableSet", "Pair", "Triple", "Array", "IntArray",
        ),
    )

    val swift = LanguageSpec(
        id = "swift",
        displayName = "Swift",
        fileExtension = "swift",
        keywords = setOf(
            "associatedtype", "class", "deinit", "enum", "extension", "fileprivate", "func",
            "import", "init", "inout", "internal", "let", "open", "operator", "private",
            "protocol", "public", "rethrows", "static", "struct", "subscript", "typealias",
            "var", "break", "case", "continue", "default", "defer", "do", "else",
            "fallthrough", "for", "guard", "if", "in", "repeat", "return", "switch",
            "where", "while", "as", "catch", "is", "throw", "throws", "try", "await",
            "async", "nil", "true", "false", "self", "Self", "super", "some", "any",
        ),
        types = setOf(
            "Int", "Double", "Float", "Bool", "String", "Character", "Array", "Dictionary",
            "Set", "Optional", "Any", "AnyObject", "Void", "Result", "Error",
        ),
    )

    val sql = LanguageSpec(
        id = "sql",
        displayName = "SQL",
        fileExtension = "sql",
        keywords = setOf(
            "add", "all", "alter", "and", "as", "asc", "begin", "between", "by", "case",
            "check", "column", "commit", "constraint", "create", "cross", "database",
            "default", "delete", "desc", "distinct", "drop", "else", "end", "exists",
            "foreign", "from", "full", "group", "having", "if", "in", "index", "inner",
            "insert", "into", "is", "join", "key", "left", "like", "limit", "not", "null",
            "offset", "on", "or", "order", "outer", "primary", "references", "returning",
            "right", "rollback", "select", "set", "table", "then", "top", "transaction",
            "union", "unique", "update", "values", "view", "when", "where", "with",
        ),
        types = setOf(
            "bigint", "boolean", "char", "date", "decimal", "double", "float", "int",
            "integer", "json", "jsonb", "numeric", "real", "serial", "smallint", "text",
            "time", "timestamp", "timestamptz", "uuid", "varchar",
        ),
        lineComment = "--",
        blockComment = "/*" to "*/",
        caseSensitive = false,
    )

    val all: List<LanguageSpec> = listOf(
        cpp, c, java, python, javascript, typescript, go, rust, kotlin, swift, sql,
    )

    private val byIdMap = all.associateBy { it.id }

    /** Aliases so content authored as `c++`, `py`, `js` still highlights correctly. */
    private val aliases = mapOf(
        "c++" to "cpp",
        "cplusplus" to "cpp",
        "py" to "python",
        "python3" to "python",
        "js" to "javascript",
        "node" to "javascript",
        "ts" to "typescript",
        "golang" to "go",
        "rs" to "rust",
        "kt" to "kotlin",
        "kts" to "kotlin",
        "postgres" to "sql",
        "postgresql" to "sql",
        "psql" to "sql",
        "plaintext" to "text",
        "txt" to "text",
    )

    fun byId(id: String?): LanguageSpec? {
        if (id.isNullOrBlank()) return null
        val key = id.trim().lowercase().removePrefix("language-")
        byIdMap[key]?.let { return it }
        val alias = aliases[key] ?: return null
        return byIdMap[alias]
    }

    fun displayName(id: String?): String =
        byId(id)?.displayName ?: (id?.takeIf { it.isNotBlank() } ?: "Text")

    /** Options for the "syntax language" pickers. */
    val pickerOptions: List<Pair<String, String>> = all.map { it.id to it.displayName }
}
