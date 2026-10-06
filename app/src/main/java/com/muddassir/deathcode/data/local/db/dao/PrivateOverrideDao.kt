package com.muddassir.deathcode.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivateOverrideDao {

    @Query("SELECT * FROM private_overrides WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getForNode(nodeId: String): PrivateOverrideEntity?

    @Query("SELECT * FROM private_overrides WHERE nodeId = :nodeId LIMIT 1")
    fun observeForNode(nodeId: String): Flow<PrivateOverrideEntity?>

    @Query("SELECT * FROM private_overrides")
    suspend fun getAll(): List<PrivateOverrideEntity>

    @Query("SELECT COUNT(*) FROM private_overrides")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: PrivateOverrideEntity)

    @Query("DELETE FROM private_overrides WHERE nodeId = :nodeId")
    suspend fun deleteForNode(nodeId: String)
}
