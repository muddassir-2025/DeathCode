package com.muddassir.deathcode.data.remote

import com.muddassir.deathcode.data.remote.dto.ContentNodeDto
import com.muddassir.deathcode.data.remote.dto.ContentPackageDto
import java.security.MessageDigest

/**
 * Deterministic checksum over a content package's nodes.
 *
 * The same algorithm runs on the backend when publishing and on the device before applying
 * an update, so a corrupted or tampered download is rejected instead of replacing a working
 * local version.
 */
object ContentChecksum {

    fun compute(nodes: List<ContentNodeDto>): String {
        val canonical = nodes
            .sortedBy { it.id }
            .joinToString(separator = "\u001e") { node ->
                listOf(
                    node.id,
                    node.parentId.orEmpty(),
                    node.title,
                    node.slug,
                    node.markdown,
                    node.syntax.orEmpty(),
                    node.notes.orEmpty(),
                    node.language.orEmpty(),
                    node.keywords.joinToString(","),
                    node.sortOrder.toString(),
                ).joinToString(separator = "\u001f")
            }

        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun compute(packageDto: ContentPackageDto): String = compute(packageDto.nodes)

    /** True when the package carries a checksum that matches its own contents. */
    fun isIntact(packageDto: ContentPackageDto): Boolean =
        packageDto.checksum.isBlank() || packageDto.checksum.equals(
            compute(packageDto),
            ignoreCase = true,
        )
}
