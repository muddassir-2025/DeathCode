package com.muddassir.deathcode.core

/**
 * Keywords are stored as a single comma separated column. These helpers keep the parsing
 * rules (trim, lowercase, de-duplicate, preserve order) in one place so the keyboard
 * index and the search index always agree.
 */
object KeywordList {

    fun parse(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',', ';', '\n')
            .asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    fun serialize(keywords: Iterable<String>): String =
        keywords.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")

    fun of(vararg keywords: String): String = serialize(keywords.toList())
}
