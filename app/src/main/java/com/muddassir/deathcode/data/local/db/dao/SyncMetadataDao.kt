package com.muddassir.deathcode.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muddassir.deathcode.data.local.db.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Query("SELECT value FROM sync_metadata WHERE key = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM sync_metadata WHERE key = :key LIMIT 1")
    fun observeValue(key: String): Flow<String?>

    @Query("SELECT * FROM sync_metadata")
    suspend fun getAll(): List<SyncMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE key = :key")
    suspend fun delete(key: String)
}
