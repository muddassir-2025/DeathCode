package com.muddassir.deathcode.data.repository

import androidx.room.withTransaction
import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.core.newId
import com.muddassir.deathcode.data.local.db.DeathCodeDatabase
import com.muddassir.deathcode.data.local.db.dao.PrivateOverrideDao
import com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity
import kotlinx.coroutines.flow.Flow

/**
 * The user's private personalization layer.
 *
 * Official and community content is never modified — instead a private note, personal
 * template, personal syntax variant and personal keywords are attached *beside* it.
 * Nothing in this repository is ever uploaded automatically.
 */
class PersonalRepository(
    private val db: DeathCodeDatabase,
    private val overrideDao: PrivateOverrideDao,
    private val contentRepository: ContentRepository,
) {

    fun observe(nodeId: String): Flow<PrivateOverrideEntity?> = overrideDao.observeForNode(nodeId)

    suspend fun get(nodeId: String): PrivateOverrideEntity? = overrideDao.getForNode(nodeId)

    suspend fun count(): Int = overrideDao.count()

    suspend fun save(
        nodeId: String,
        note: String,
        template: String?,
        syntax: String?,
        keywords: List<String>,
        language: String?,
    ) {
        val existing = overrideDao.getForNode(nodeId)
        val entity = PrivateOverrideEntity(
            id = existing?.id ?: newId("ov"),
            nodeId = nodeId,
            note = note,
            template = template?.takeIf { it.isNotBlank() },
            syntax = syntax?.takeIf { it.isNotBlank() },
            keywords = KeywordList.serialize(keywords),
            language = language,
            updatedAt = System.currentTimeMillis(),
        )

        if (entity.isEmpty) {
            clear(nodeId)
            return
        }

        db.withTransaction {
            overrideDao.upsert(entity)
            // The private note/template/keyword must be immediately searchable offline.
            contentRepository.reindex(nodeId)
        }
    }

    suspend fun clear(nodeId: String) {
        db.withTransaction {
            overrideDao.deleteForNode(nodeId)
            contentRepository.reindex(nodeId)
        }
    }
}
