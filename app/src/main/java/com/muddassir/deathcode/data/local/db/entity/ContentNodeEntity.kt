package com.muddassir.deathcode.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.muddassir.deathcode.domain.model.ContentSource

/**
 * A single node in the recursive Death Code content tree.
 *
 * The tree has **no depth limit**. Any node may act as a container (by having children)
 * or as a content card (by carrying [markdown]/[syntax]/[notes]/[keywords]). Both are
 * allowed simultaneously, and both manual creation and markdown import are available at
 * every node.
 *
 * A `null` [parentId] marks a root category.
 */
@Entity(
    tableName = "content_nodes",
    foreignKeys = [
        ForeignKey(
            entity = ContentNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("parentId"),
        Index("source"),
        Index(value = ["source", "parentId"]),
        Index("remoteId"),
        Index("slug"),
    ],
)
data class ContentNodeEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val title: String,
    val slug: String,
    /** Markdown body for the card / reading mode. */
    val markdown: String = "",
    /** Primary code snippet shown on the card. */
    val syntax: String? = null,
    /** Official / community notes. Private user notes live in `private_overrides`. */
    val notes: String? = null,
    /** Primary programming language of [syntax], e.g. `cpp`, `python`. */
    val language: String? = null,
    /** Comma separated keyboard keywords, e.g. `for,forloop,loop`. */
    val keywords: String = "",
    val source: ContentSource = ContentSource.OFFICIAL,
    /** Backend identity for Community content the user authored (nullable). */
    val ownerId: String? = null,
    val sortOrder: Int = 0,
    /** Stable identity on the backend for synchronized OFFICIAL / COMMUNITY content. */
    val remoteId: String? = null,
    /** Content version this node was published in (OFFICIAL / COMMUNITY only). */
    val contentVersion: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    val isReadOnly: Boolean get() = source.isReadOnlyForUser
}
