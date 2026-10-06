package com.muddassir.deathcode.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * A published content release.
 *
 * The format carries enough to reconstruct hierarchy, markdown, syntax, notes, keywords,
 * language and ordering, and it is versioned so delta updates can be added later without an
 * architectural rewrite.
 */
@Serializable
data class ContentPackageDto(
    val packageVersion: Long,
    val generatedAt: Long = 0L,
    val checksum: String = "",
    /** `OFFICIAL` or `COMMUNITY`. */
    val kind: String = "OFFICIAL",
    /**
     * `true` when [nodes] is the **complete** published set for this source, meaning
     * content absent from the package has been unpublished and should be removed locally.
     *
     * `false` marks a partial/delta publication: nodes are added or updated and nothing is
     * deleted. This is what lets the format support delta updates later without a rewrite.
     */
    val fullSnapshot: Boolean = true,
    val nodes: List<ContentNodeDto> = emptyList(),
)

@Serializable
data class ContentNodeDto(
    val id: String,
    val parentId: String? = null,
    val title: String,
    val slug: String = "",
    val markdown: String = "",
    val syntax: String? = null,
    val notes: String? = null,
    val language: String? = null,
    val keywords: List<String> = emptyList(),
    val sortOrder: Int = 0,
)

/** Lightweight "is there anything new?" response. */
@Serializable
data class ManifestDto(
    val version: Long,
    val generatedAt: Long = 0L,
    val checksum: String = "",
)

// ------------------------------------------------------------------ auth / community

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class SignupRequest(val email: String, val password: String, val displayName: String? = null)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val isAdmin: Boolean = false,
)

@Serializable
data class CommunitySubmissionDto(
    val id: String? = null,
    val title: String,
    val markdown: String,
    val syntax: String? = null,
    val language: String? = null,
    val keywords: List<String> = emptyList(),
)

@Serializable
data class CommunitySubmissionResponse(
    val id: String,
    val status: String,
    val message: String? = null,
)

/** Standard error envelope returned by the API. */
@Serializable
data class ApiErrorDto(val error: String, val detail: String? = null)
