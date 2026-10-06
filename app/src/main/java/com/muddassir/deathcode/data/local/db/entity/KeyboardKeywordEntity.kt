package com.muddassir.deathcode.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.muddassir.deathcode.domain.model.ContentSource

/**
 * A keyword that the Death Code Keyboard can match while the user types.
 *
 * Kept as a dedicated, indexed table (rather than scanning node/snippet columns) so the
 * IME can resolve suggestions with a fast prefix lookup on every keystroke, fully offline.
 */
@Entity(
    tableName = "keyboard_keywords",
    indices = [
        Index("keyword"),
        Index("language"),
        Index(value = ["targetType", "targetId"]),
    ],
)
data class KeyboardKeywordEntity(
    @PrimaryKey val id: String,
    val keyword: String,
    /** `NODE` or `SNIPPET`. */
    val targetType: String,
    val targetId: String,
    /** Empty string means "matches any language". */
    val language: String = "",
    val source: ContentSource = ContentSource.PRIVATE,
    val usageCount: Int = 0,
    val updatedAt: Long = 0,
) {
    companion object {
        const val TARGET_NODE = "NODE"
        const val TARGET_SNIPPET = "SNIPPET"
    }
}
