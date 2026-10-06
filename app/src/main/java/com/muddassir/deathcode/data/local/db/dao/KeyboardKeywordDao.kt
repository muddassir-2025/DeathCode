package com.muddassir.deathcode.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muddassir.deathcode.data.local.db.entity.KeyboardKeywordEntity
import com.muddassir.deathcode.domain.model.ContentSource

@Dao
interface KeyboardKeywordDao {

    /** Prefix lookup used on every keystroke; the index on `keyword` keeps this cheap. */
    @Query(
        """
        SELECT * FROM keyboard_keywords
        WHERE keyword LIKE :prefix
        ORDER BY LENGTH(keyword) ASC, usageCount DESC
        LIMIT :limit
        """,
    )
    suspend fun findByPrefix(prefix: String, limit: Int): List<KeyboardKeywordEntity>

    @Query(
        """
        SELECT * FROM keyboard_keywords
        WHERE keyword = :keyword AND (language = :language OR language = '')
        ORDER BY usageCount DESC
        LIMIT :limit
        """,
    )
    suspend fun findExact(keyword: String, language: String, limit: Int): List<KeyboardKeywordEntity>

    @Query("SELECT * FROM keyboard_keywords WHERE targetType = :type AND targetId = :targetId")
    suspend fun getForTarget(type: String, targetId: String): List<KeyboardKeywordEntity>

    @Query("SELECT * FROM keyboard_keywords WHERE source IN (:sources)")
    suspend fun getBySources(sources: List<ContentSource>): List<KeyboardKeywordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(keywords: List<KeyboardKeywordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(keyword: KeyboardKeywordEntity)

    @Query("DELETE FROM keyboard_keywords WHERE targetType = :type AND targetId = :targetId")
    suspend fun deleteForTarget(type: String, targetId: String)

    @Query("DELETE FROM keyboard_keywords WHERE targetType = :type AND targetId IN (:targetIds)")
    suspend fun deleteForTargets(type: String, targetIds: List<String>)

    @Query("UPDATE keyboard_keywords SET usageCount = usageCount + 1 WHERE targetType = :type AND targetId = :targetId")
    suspend fun recordUsage(type: String, targetId: String)
}
