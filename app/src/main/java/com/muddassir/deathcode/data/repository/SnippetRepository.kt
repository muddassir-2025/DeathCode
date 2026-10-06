package com.muddassir.deathcode.data.repository

import androidx.room.withTransaction
import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.core.newId
import com.muddassir.deathcode.data.local.db.DeathCodeDatabase
import com.muddassir.deathcode.data.local.db.dao.KeyboardKeywordDao
import com.muddassir.deathcode.data.local.db.dao.SnippetDao
import com.muddassir.deathcode.data.local.db.entity.KeyboardKeywordEntity
import com.muddassir.deathcode.data.local.db.entity.SnippetEntity
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.Flow

/** Fields accepted when creating or editing a reusable snippet. */
data class SnippetDraft(
    val title: String,
    val language: String,
    val body: String,
    val keywords: List<String>,
    val nodeId: String? = null,
)

/**
 * Reusable, language-scoped code templates surfaced by the Death Code Keyboard.
 *
 * Snippets are indexed into `keyboard_keywords` so suggestion lookup stays a single
 * indexed query instead of scanning every template on each keystroke.
 */
class SnippetRepository(
    private val db: DeathCodeDatabase,
    private val snippetDao: SnippetDao,
    private val keywordDao: KeyboardKeywordDao,
) {

    fun observeAll(): Flow<List<SnippetEntity>> = snippetDao.observeAll()

    suspend fun getAll(): List<SnippetEntity> = snippetDao.getAll()

    suspend fun getById(id: String): SnippetEntity? = snippetDao.getById(id)

    suspend fun getForNode(nodeId: String): List<SnippetEntity> = snippetDao.getForNode(nodeId)

    suspend fun count(): Int = snippetDao.count()

    suspend fun save(
        draft: SnippetDraft,
        source: ContentSource = ContentSource.PRIVATE,
        id: String? = null,
    ): String {
        val existing = id?.let { snippetDao.getById(it) }
        val snippetId = existing?.id ?: newId("snip")
        val now = System.currentTimeMillis()
        val entity = SnippetEntity(
            id = snippetId,
            nodeId = draft.nodeId,
            title = draft.title.trim().ifEmpty { "Snippet" },
            language = draft.language.lowercase(),
            body = draft.body,
            keywords = KeywordList.serialize(draft.keywords),
            source = source,
            sortOrder = existing?.sortOrder ?: 0,
            usageCount = existing?.usageCount ?: 0,
            lastUsedAt = existing?.lastUsedAt ?: 0,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        db.withTransaction {
            snippetDao.upsert(entity)
            writeKeywordIndex(entity)
        }
        return snippetId
    }

    suspend fun delete(id: String) {
        db.withTransaction {
            snippetDao.deleteById(id)
            keywordDao.deleteForTarget(KeyboardKeywordEntity.TARGET_SNIPPET, id)
        }
    }

    suspend fun recordUsage(id: String) {
        snippetDao.recordUsage(id, System.currentTimeMillis())
    }

    private suspend fun writeKeywordIndex(snippet: SnippetEntity) {
        val keywords = KeywordList.parse(snippet.keywords)
        keywordDao.deleteForTarget(KeyboardKeywordEntity.TARGET_SNIPPET, snippet.id)
        if (keywords.isEmpty()) return
        keywordDao.upsertAll(
            keywords.map { keyword ->
                KeyboardKeywordEntity(
                    id = "kw-snippet-${snippet.id}-$keyword",
                    keyword = keyword,
                    targetType = KeyboardKeywordEntity.TARGET_SNIPPET,
                    targetId = snippet.id,
                    language = snippet.language,
                    source = snippet.source,
                    updatedAt = snippet.updatedAt,
                )
            },
        )
    }
}
