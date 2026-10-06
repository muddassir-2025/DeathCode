package com.muddassir.deathcode.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.Flow

/** Lightweight row used to resolve breadcrumb paths without loading full nodes. */
data class AncestryRow(
    val id: String,
    val parentId: String?,
    val title: String,
)

/** One row of an ancestor breadcrumb path. */
data class PathRow(
    val id: String,
    val title: String,
    val slug: String,
    val depth: Int,
)

/** Minimal projection used while ordering a subtree in memory. */
data class NodeOrderRow(
    val id: String,
    val parentId: String?,
    val sortOrder: Int,
    val title: String,
)

@Dao
interface ContentNodeDao {

    @Query("SELECT * FROM content_nodes WHERE parentId IS :parentId ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    fun observeChildren(parentId: String?): Flow<List<ContentNodeEntity>>

    @Query("SELECT * FROM content_nodes WHERE parentId IS :parentId ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    suspend fun getChildren(parentId: String?): List<ContentNodeEntity>

    @Query("SELECT * FROM content_nodes WHERE parentId IS NULL AND source = :source ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    fun observeRoots(source: ContentSource): Flow<List<ContentNodeEntity>>

    @Query("SELECT * FROM content_nodes WHERE parentId IS NULL AND source = :source ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    suspend fun getRoots(source: ContentSource): List<ContentNodeEntity>

    @Query("SELECT * FROM content_nodes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContentNodeEntity?

    @Query("SELECT * FROM content_nodes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ContentNodeEntity?>

    @Query("SELECT * FROM content_nodes WHERE source = :source")
    suspend fun getAllBySource(source: ContentSource): List<ContentNodeEntity>

    @Query("SELECT * FROM content_nodes WHERE source = :source")
    fun observeAllBySource(source: ContentSource): Flow<List<ContentNodeEntity>>

    @Query("SELECT COUNT(*) FROM content_nodes WHERE parentId IS :parentId")
    suspend fun countChildren(parentId: String?): Int

    @Query("SELECT * FROM content_nodes")
    suspend fun getAll(): List<ContentNodeEntity>

    // NOTE: node inserts deliberately ABORT on conflict instead of using REPLACE.
    // `INSERT OR REPLACE` performs a delete + insert, which would fire the self
    // referencing ON DELETE CASCADE and silently destroy a node's whole subtree (and the
    // user's private notes attached to it). Callers upsert explicitly through
    // [upsert] instead.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(node: ContentNodeEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(nodes: List<ContentNodeEntity>)

    @Update
    suspend fun update(node: ContentNodeEntity)

    @Query("DELETE FROM content_nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM content_nodes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    /** Explicit upsert that never triggers a cascade delete. */
    suspend fun upsert(node: ContentNodeEntity) {
        if (getById(node.id) == null) insert(node) else update(node)
    }

    @Query("DELETE FROM content_nodes WHERE source = :source")
    suspend fun deleteBySource(source: ContentSource)

    @Query(
        """
        UPDATE content_nodes
        SET parentId = :parentId, sortOrder = :sortOrder, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateParent(id: String, parentId: String?, sortOrder: Int, updatedAt: Long)

    @Query(
        """
        UPDATE content_nodes
        SET title = :title, slug = :slug, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun rename(id: String, title: String, slug: String, updatedAt: Long)

    /** Ids of [id] and all of its descendants (any depth). */
    @Query(
        """
        WITH RECURSIVE subtree(id) AS (
            SELECT id FROM content_nodes WHERE id = :id
            UNION ALL
            SELECT c.id FROM content_nodes c JOIN subtree s ON c.parentId = s.id
        )
        SELECT id FROM subtree
        """,
    )
    suspend fun getSubtreeIds(id: String): List<String>

    @Query(
        """
        WITH RECURSIVE subtree(id) AS (
            SELECT id FROM content_nodes WHERE id = :id
            UNION ALL
            SELECT c.id FROM content_nodes c JOIN subtree s ON c.parentId = s.id
        )
        SELECT * FROM content_nodes WHERE id IN (SELECT id FROM subtree)
        """,
    )
    suspend fun getSubtree(id: String): List<ContentNodeEntity>

    /**
     * Ancestor chain for [id] from the root down to the node itself (depth 0 == the node).
     * Callers should reverse to obtain root -> node order.
     */
    @Query(
        """
        WITH RECURSIVE up(id, parentId, title, slug, depth) AS (
            SELECT id, parentId, title, slug, 0 FROM content_nodes WHERE id = :id
            UNION ALL
            SELECT c.id, c.parentId, c.title, c.slug, u.depth + 1
            FROM content_nodes c JOIN up u ON c.id = u.parentId
        )
        SELECT * FROM up ORDER BY depth DESC
        """,
    )
    suspend fun getPath(id: String): List<PathRow>

    @Query("SELECT MAX(sortOrder) FROM content_nodes WHERE parentId IS :parentId")
    suspend fun maxSortOrder(parentId: String?): Int?

    @Query("SELECT id, parentId, sortOrder, title FROM content_nodes WHERE parentId IS :parentId ORDER BY sortOrder ASC")
    suspend fun getOrderRows(parentId: String?): List<NodeOrderRow>

    /** Fallback substring search used when FTS yields nothing. */
    @Query(
        """
        SELECT * FROM content_nodes
        WHERE source IN (:sources)
          AND (title LIKE :like OR keywords LIKE :like OR syntax LIKE :like
               OR notes LIKE :like OR markdown LIKE :like)
        LIMIT :limit
        """,
    )
    suspend fun searchLike(sources: List<ContentSource>, like: String, limit: Int): List<ContentNodeEntity>

    @Query("SELECT * FROM content_nodes WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ContentNodeEntity>

    @Query("SELECT COUNT(*) FROM content_nodes WHERE source = :source")
    suspend fun countBySource(source: ContentSource): Int

    @Query("SELECT id, parentId, title FROM content_nodes")
    suspend fun getAncestryRows(): List<AncestryRow>
}
