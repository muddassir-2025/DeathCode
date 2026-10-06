package com.muddassir.deathcode.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A locally stored Community submission.
 *
 * Only content the user explicitly submits ever leaves the device. The submission is kept
 * locally so the user can see its moderation state (and so failed uploads can be retried)
 * without the backend being part of the normal runtime path.
 */
@Entity(
    tableName = "community_submissions",
    indices = [
        Index("status"),
        Index("remoteId"),
    ],
)
data class CommunitySubmissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val markdown: String,
    val syntax: String? = null,
    val language: String? = null,
    val keywords: String = "",
    val status: String = STATUS_DRAFT,
    /** Server side submission id once accepted. */
    val remoteId: String? = null,
    /** Moderation message returned by the admin/backend, if any. */
    val reviewMessage: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    companion object {
        /** Not submitted yet. */
        const val STATUS_DRAFT = "DRAFT"
        /** Uploaded, waiting for admin review. */
        const val STATUS_PENDING = "PENDING"
        /** Approved and therefore publicly visible / synchronized. */
        const val STATUS_APPROVED = "APPROVED"
        /** Rejected by the admin. */
        const val STATUS_REJECTED = "REJECTED"
        /** Upload failed; safe to retry. */
        const val STATUS_FAILED = "FAILED"
    }
}
