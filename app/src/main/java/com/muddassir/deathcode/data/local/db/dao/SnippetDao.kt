package com.muddassir.deathcode.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.muddassir.deathcode.data.local.db.entity.SnippetEntity
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {

    @Query("SELECT * FROM snippets ORDER BY usageCount DESC, sortOrder ASC, title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets")
    suspend fun getAll(): List<SnippetEntity>

    @Query("SELECT * FROM snippets WHERE source IN (:sources)")
    suspend fun getBySources(sources: List<ContentSource>): List<SnippetEntity>

    @Query("SELECT * FROM snippets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SnippetEntity?

    @Query("SELECT * FROM snippets WHERE nodeId = :nodeId")
    suspend fun getForNode(nodeId: String): List<SnippetEntity>

    @Query("SELECT * FROM snippets WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SnippetEntity>

    @Query("SELECT COUNT(*) FROM snippets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snippet: SnippetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(snippets: List<SnippetEntity>)

    @Update
    suspend fun update(snippet: SnippetEntity)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE snippets SET usageCount = usageCount + 1, lastUsedAt = :now WHERE id = :id")
    suspend fun recordUsage(id: String, now: Long)
}
