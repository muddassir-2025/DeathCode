package com.muddassir.deathcode.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Small key/value store for synchronization state.
 *
 * Examples: `official_version`, `official_checksum`, `community_version`,
 * `last_sync_at`, `last_sync_status`.
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = 0,
) {
    companion object {
        const val KEY_OFFICIAL_VERSION = "official_version"
        const val KEY_OFFICIAL_CHECKSUM = "official_checksum"
        const val KEY_COMMUNITY_VERSION = "community_version"
        const val KEY_COMMUNITY_CHECKSUM = "community_checksum"
        const val KEY_LAST_SYNC_AT = "last_sync_at"
        const val KEY_LAST_SYNC_STATUS = "last_sync_status"
        const val KEY_BUNDLED_SEEDED = "bundled_seeded"
    }
}
