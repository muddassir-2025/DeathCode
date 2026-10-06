package com.muddassir.deathcode.data.local.search

import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.core.TextNormalizer
import com.muddassir.deathcode.data.local.db.dao.AncestryRow
import com.muddassir.deathcode.data.local.db.dao.ContentNodeDao
import com.muddassir.deathcode.data.local.db.dao.PrivateOverrideDao
import com.muddassir.deathcode.data.local.db.dao.SearchDao
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity
import com.muddassir.deathcode.domain.model.ContentSource

/**
 * Deterministic, fully offline search over the local Room database.
 *
 * Pipeline:
 * ```
 * query -> normalize -> tokenize -> drop stop words -> FTS4 prefix match
 *       -> (LIKE fallback when FTS is thin) -> rank in memory -> results
 * ```
 *
 * The backend is never contacted. Ranking is a transparent weighted sum so it can be
 * unit tested and reasoned about — see [SearchRanker] for the weights.
 */
class SearchEngine(
    private val nodeDao: ContentNodeDao,
    private val searchDao: SearchDao,
    private val overrideDao: PrivateOverrideDao,
) {

    suspend fun search(
        rawQuery: String,
        filters: SearchFilters = SearchFilters(),
        limit: Int = 50,
    ): List<SearchResult> {
        if (rawQuery.isBlank()) return emptyList()
        val processed = QueryProcessor.process(rawQuery)
        val sources = filters.sources.toList().ifEmpty { ContentSource.entries.toList() }

        // --- candidate generation -------------------------------------------------
        val candidates = LinkedHashMap<String, ContentNodeEntity>()

        val match = processed.ftsMatch
        if (match != null) {
            val ids: List<String> = runCatching { searchDao.matchNodeIds(match, CANDIDATE_LIMIT) }
                .getOrDefault(emptyList())
            if (ids.isNotEmpty()) {
                val nodes = nodeDao.getByIds(ids)
                nodes.forEach { node -> candidates[node.id] = node }
            }
        }

        // LIKE fallback keeps search useful when FTS returns few/none (e.g. substrings).
        if (candidates.size < FALLBACK_THRESHOLD) {
            for (term in processed.terms.take(MAX_FALLBACK_TERMS)) {
                val like = "%${escapeLike(term)}%"
                val hits = runCatching { nodeDao.searchLike(sources, like, CANDIDATE_LIMIT) }
                    .getOrDefault(emptyList())
                hits.forEach { node -> candidates[node.id] = node }
            }
        }

        // --- ancestry + private personalization -----------------------------------
        val ancestry = nodeDao.getAncestryRows().associateBy { it.id }
        val overrides = overrideDao.getAll().associateBy { it.nodeId }

        val sourceFiltered = candidates.values.filter { it.source in filters.sources }
        val results = sourceFiltered.mapNotNull { node ->
            val path = resolvePath(node, ancestry)
            val override = overrides[node.id]
            SearchRanker.rank(processed, node, override, path)
        }

        return results
            .sortedWith(
                compareByDescending<SearchResult> { it.score }
                    .thenBy { it.node.title.length }
                    .thenBy { it.node.title },
            )
            .take(limit)
    }

    /** Gives the keyboard/suggestion UI a cheap "does anything match" probe. */
    suspend fun quickMatch(prefix: String, sources: Set<ContentSource>, limit: Int = 10): List<SearchResult> =
        search(prefix, SearchFilters(sources), limit)

    /** Walks the lightweight ancestry map from the node up to its root. */
    private fun resolvePath(
        node: ContentNodeEntity,
        ancestry: Map<String, AncestryRow>,
    ): List<String> {
        val self = ancestry[node.id]
        val titles = ArrayDeque<String>()
        titles.addFirst(self?.title ?: node.title)
        var parentId = self?.parentId ?: node.parentId
        var guard = 0
        while (parentId != null && guard++ < MAX_DEPTH) {
            val row = ancestry[parentId] ?: break
            titles.addFirst(row.title)
            parentId = row.parentId
        }
        return titles.toList()
    }

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    companion object {
        private const val CANDIDATE_LIMIT = 300
        private const val FALLBACK_THRESHOLD = 8
        private const val MAX_FALLBACK_TERMS = 4
        private const val MAX_DEPTH = 200
    }
}
