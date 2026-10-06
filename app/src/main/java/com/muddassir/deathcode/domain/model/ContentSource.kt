package com.muddassir.deathcode.domain.model

/**
 * The ownership world a piece of content belongs to.
 *
 * - [OFFICIAL]  content distributed by the Super Admin. Read-only for normal users,
 *               but users may attach private notes/templates to it.
 * - [PRIVATE]   content created by the local user. Never leaves the device unless the
 *               user explicitly submits it to Community.
 * - [COMMUNITY] approved community content synchronized from the backend.
 */
enum class ContentSource {
    OFFICIAL,
    PRIVATE,
    COMMUNITY;

    val isReadOnlyForUser: Boolean get() = this != PRIVATE
}
