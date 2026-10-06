package com.muddassir.deathcode.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muddassir.deathcode.data.local.db.entity.CommunitySubmissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunitySubmissionDao {

    @Query("SELECT * FROM community_submissions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CommunitySubmissionEntity>>

    @Query("SELECT * FROM community_submissions WHERE status = :status ORDER BY updatedAt DESC")
    suspend fun getByStatus(status: String): List<CommunitySubmissionEntity>

    @Query("SELECT COUNT(*) FROM community_submissions WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT * FROM community_submissions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CommunitySubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(submission: CommunitySubmissionEntity)

    @Query("DELETE FROM community_submissions WHERE id = :id")
    suspend fun deleteById(id: String)
}
