package com.muddassir.deathcode.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user's private personalization attached to a content node.
 *
 * Official / community content can never be overwritten, but a user may attach:
 * a private note, a personal template, a personal syntax variant and personal keywords.
 * Everything here stays on the device.
 */
@Entity(
    tableName = "private_overrides",
    foreignKeys = [
        ForeignKey(
            entity = ContentNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["nodeId"], unique = true)],
)
data class PrivateOverrideEntity(
    @PrimaryKey val id: String,
    val nodeId: String,
    /** Free-form private note. */
    val note: String = "",
    /** Personal reusable template body (supports `${cursor}` / `${name}` placeholders). */
    val template: String? = null,
    /** Personal syntax variant shown next to the official syntax. */
    val syntax: String? = null,
    /** Personal keywords that the keyboard should also suggest. */
    val keywords: String = "",
    /** Language of [template]/[syntax]. */
    val language: String? = null,
    val updatedAt: Long = 0,
) {
    val isEmpty: Boolean
        get() = note.isBlank() &&
            template.isNullOrBlank() &&
            syntax.isNullOrBlank() &&
            keywords.isBlank()
}
