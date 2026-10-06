package com.muddassir.deathcode.data.repository

import com.muddassir.deathcode.data.local.db.dao.ContentNodeDao
import com.muddassir.deathcode.data.local.db.dao.KeyboardKeywordDao
import com.muddassir.deathcode.data.local.db.dao.PrivateOverrideDao
import com.muddassir.deathcode.data.local.db.dao.SnippetDao
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.KeyboardKeywordEntity
import com.muddassir.deathcode.data.local.db.entity.SnippetEntity
import com.muddassir.deathcode.domain.model.ContentSource
import com.muddassir.deathcode.keyboard.snippets.TemplateEngine

/**
 * A snippet the keyboard can offer for the word currently being typed.
 *
 * [template] is the raw template; the IME expands it with [TemplateEngine] before
 * inserting it, so cursor/placeholder handling stays in one place.
 */
data class KeyboardSuggestion(
    val targetType: String,
    val targetId: String,
    val title: String,
    val template: String,
    val language: String?,
    val source: ContentSource,
    val exact: Boolean,
    val score: Double,
) {
    val key: String get() = "$targetType:$targetId"
    val preview: String get() = TemplateEngine.preview(template)
}

/**
 * Suggestion ranking for the Death Code Keyboard, exactly as specified:
 *
 * 1. exact typed keyword
 * 2. prefix match
 * 3. personal snippets
 * 4. frequently used snippets
 * 5. official Death Code snippets
 * 6. community snippets
 *
 * Runs entirely off the local Room database — no network request is ever needed.
 */
class KeyboardRepository(
    private val keywordDao: KeyboardKeywordDao,
    private val snippetDao: SnippetDao,
    private val nodeDao: ContentNodeDao,
    private val overrideDao: PrivateOverrideDao,
) {

    suspend fun suggestions(
        typed: String,
        language: String?,
        sources: Set<ContentSource>,
        limit: Int = 8,
    ): List<KeyboardSuggestion> {
        val word = typed.trim().lowercase()
        if (word.length < 1) return emptyList()

        val exact = keywordDao.findExact(word, language.orEmpty(), limit * 2)
        val prefix = keywordDao.findByPrefix("$word%", limit * 3)

        val matched = LinkedHashMap<Pair<String, String>, KeyboardKeywordEntity>()
        (exact + prefix).forEach { keyword ->
            matched.putIfAbsent(keyword.targetType to keyword.targetId, keyword)
        }
        if (matched.isEmpty()) return emptyList()

        val nodeIds = matched.values.filter { it.targetType == KeyboardKeywordEntity.TARGET_NODE }
            .map { it.targetId }.distinct()
        val snippetIds = matched.values.filter { it.targetType == KeyboardKeywordEntity.TARGET_SNIPPET }
            .map { it.targetId }.distinct()

        val nodes: Map<String, ContentNodeEntity> =
            if (nodeIds.isEmpty()) emptyMap() else nodeDao.getByIds(nodeIds).associateBy { it.id }
        val snippets: Map<String, SnippetEntity> =
            if (snippetIds.isEmpty()) emptyMap() else snippetDao.getByIds(snippetIds).associateBy { it.id }

        val results = mutableListOf<KeyboardSuggestion>()

        matched.values.forEach { keyword ->
            if (keyword.source !in sources) return@forEach
            val exactMatch = keyword.keyword.equals(word, ignoreCase = true)

            when (keyword.targetType) {
                KeyboardKeywordEntity.TARGET_SNIPPET -> {
                    val snippet = snippets[keyword.targetId] ?: return@forEach
                    if (snippet.body.isBlank()) return@forEach
                    results += KeyboardSuggestion(
                        targetType = keyword.targetType,
                        targetId = snippet.id,
                        title = snippet.title,
                        template = snippet.body,
                        language = snippet.language.ifBlank { null },
                        source = snippet.source,
                        exact = exactMatch,
                        score = scoreSuggestion(
                            exact = exactMatch,
                            keyword = keyword,
                            source = snippet.source,
                            language = snippet.language.ifBlank { null },
                            activeLanguage = language,
                            usageCount = maxOf(snippet.usageCount, keyword.usageCount),
                        ),
                    )
                }

                KeyboardKeywordEntity.TARGET_NODE -> {
                    val node = nodes[keyword.targetId] ?: return@forEach
                    val override = overrideDao.getForNode(node.id)
                    val template = override?.template?.takeIf { it.isNotBlank() }
                        ?: node.syntax?.takeIf { it.isNotBlank() }
                        ?: return@forEach
                    val effectiveSource = if (override?.template?.isNotBlank() == true) {
                        ContentSource.PRIVATE
                    } else {
                        node.source
                    }
                    results += KeyboardSuggestion(
                        targetType = keyword.targetType,
                        targetId = node.id,
                        title = node.title,
                        template = template,
                        language = (override?.language ?: node.language),
                        source = effectiveSource,
                        exact = exactMatch,
                        score = scoreSuggestion(
                            exact = exactMatch,
                            keyword = keyword,
                            source = effectiveSource,
                            language = (override?.language ?: node.language),
                            activeLanguage = language,
                            usageCount = keyword.usageCount,
                        ),
                    )
                }
            }
        }

        return results
            .distinctBy { it.key }
            .sortedWith(
                compareByDescending<KeyboardSuggestion> { it.score }
                    .thenByDescending { it.exact }
                    .thenBy { it.title.length },
            )
            .take(limit)
    }

    suspend fun recordUsage(suggestion: KeyboardSuggestion) {
        keywordDao.recordUsage(suggestion.targetType, suggestion.targetId)
        if (suggestion.targetType == KeyboardKeywordEntity.TARGET_SNIPPET) {
            snippetDao.recordUsage(suggestion.targetId, System.currentTimeMillis())
        }
    }

    private fun scoreSuggestion(
        exact: Boolean,
        keyword: KeyboardKeywordEntity,
        source: ContentSource,
        language: String?,
        activeLanguage: String?,
        usageCount: Int,
    ): Double {
        var score = if (exact) 100.0 else 40.0

        // Personal snippets outrank official and community content.
        score += when (source) {
            ContentSource.PRIVATE -> 30.0
            ContentSource.OFFICIAL -> 10.0
            ContentSource.COMMUNITY -> 5.0
        }

        // Frequently used snippets float up, but never above an exact personal match.
        score += minOf(usageCount, 10) * 2.0

        // Prefer the active keyboard language; language-neutral entries still apply.
        score += when {
            activeLanguage.isNullOrBlank() -> 0.0
            language.equals(activeLanguage, ignoreCase = true) -> 20.0
            language.isNullOrBlank() -> 5.0
            else -> -10.0
        }

        // Shorter, more specific keywords are better triggers.
        score += when (keyword.keyword.length) {
            0, 1 -> 6.0
            in 2..4 -> 4.0
            else -> 0.0
        }

        return score
    }
}
