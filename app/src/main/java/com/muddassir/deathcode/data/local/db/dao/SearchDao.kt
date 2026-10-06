package com.muddassir.deathcode.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.muddassir.deathcode.data.local.db.entity.NodeFtsEntity

/**
 * Reads and maintains the standalone FTS4 index.
 *
 * Writes to this table are always performed inside the same transaction that mutates
 * `content_nodes` so the index can never drift from the tree.
 */
@Dao
interface SearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: NodeFtsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(documents: List<NodeFtsEntity>)

    @Query("DELETE FROM node_fts WHERE nodeId = :nodeId")
    suspend fun deleteByNodeId(nodeId: String)

    @Query("DELETE FROM node_fts WHERE nodeId IN (:nodeIds)")
    suspend fun deleteByNodeIds(nodeIds: List<String>)

    @Query("DELETE FROM node_fts")
    suspend fun clear()

    @Query("SELECT nodeId FROM node_fts WHERE node_fts MATCH :matchQuery LIMIT :limit")
    suspend fun matchNodeIds(matchQuery: String, limit: Int): List<String>

    @Query("SELECT COUNT(*) FROM node_fts")
    suspend fun count(): Int

    @Query("SELECT nodeId FROM node_fts LIMIT :limit")
    suspend fun allNodeIds(limit: Int = 100000): List<String>
}
