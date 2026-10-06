package com.muddassir.deathcode.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.muddassir.deathcode.domain.model.ContentSource

/**
 * A reusable code template that the Death Code Keyboard can expand.
 *
 * Snippets are language-scoped so the same keyword (e.g. `bfs`) can resolve to different
 * templates per active keyboard language. [body] supports placeholders: `${cursor}` and
 * arbitrary `${name}` placeholders.
 */
@Entity(
    tableName = "snippets",
    indices = [
        Index("source"),
        Index("language"),
        Index("nodeId"),
    ],
)
data class SnippetEntity(
    @PrimaryKey val id: String,
    /** Optional link back to the content node this snippet belongs to. */
    val nodeId: String? = null,
    val title: String,
    val language: String,
    val body: String,
    /** Comma separated trigger keywords, e.g. `bfs,breadth`. */
    val keywords: String = "",
    val source: ContentSource = ContentSource.PRIVATE,
    val sortOrder: Int = 0,
    /** Used for "frequently used snippets first" keyboard ranking. */
    val usageCount: Int = 0,
    val lastUsedAt: Long = 0,
    val remoteId: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
