package com.muddassir.deathcode.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Full-text search index for the content tree.
 *
 * This is a standalone FTS4 table (not external content), so it is kept in sync by
 * [com.muddassir.deathcode.data.local.db.dao.ContentNodeDao] inside the same transaction
 * that mutates `content_nodes`. `nodeId` is stored but not tokenized so that UUIDs never
 * pollute search results.
 */
@Fts4(notIndexed = ["nodeId"])
@Entity(tableName = "node_fts")
data class NodeFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,
    val nodeId: String,
    val title: String,
    /** Human readable breadcrumb, e.g. `C++ DSA Fundamentals Loops`. */
    val path: String,
    val markdown: String,
    val syntax: String,
    val notes: String,
    val keywords: String,
)
