package com.muddassir.deathcode.data.remote

import com.muddassir.deathcode.data.remote.dto.ContentNodeDto
import com.muddassir.deathcode.data.remote.dto.ContentPackageDto

/**
 * Pure validation helpers for downloaded content packages.
 *
 * Kept free of Android/Room dependencies so package safety rules can be unit tested
 * directly — a malformed or corrupted download must never be allowed to reach the database.
 */
object ContentPackageValidator {

    /**
     * Orders nodes so every parent precedes its children (required because the database
     * enforces foreign keys).
     *
     * Returns `null` — i.e. rejects the whole package — when there is a duplicate id or a
     * cycle. A dangling parent reference is only fatal for a full snapshot; a partial
     * package may reference parents that already exist on the device.
     */
    fun orderParentsFirst(
        nodes: List<ContentNodeDto>,
        requireParents: Boolean = true,
    ): List<ContentNodeDto>? {
        val byId = nodes.associateBy { it.id }
        if (byId.size != nodes.size) return null

        val ordered = ArrayList<ContentNodeDto>(nodes.size)
        val visiting = HashSet<String>()
        val visited = HashSet<String>()

        fun visit(node: ContentNodeDto): Boolean {
            if (node.id in visited) return true
            if (!visiting.add(node.id)) return false // cycle
            val parentId = node.parentId
            if (parentId != null) {
                val parent = byId[parentId]
                if (parent != null) {
                    if (!visit(parent)) return false
                } else if (requireParents) {
                    return false // dangling reference in a full snapshot
                }
            }
            visiting.remove(node.id)
            visited.add(node.id)
            ordered += node
            return true
        }

        nodes.forEach { if (!visit(it)) return null }

        // Re-sort by depth, then keep the declared sibling order inside each level.
        return ordered.sortedWith(
            compareBy(
                { depthOf(it, byId) },
                { it.parentId.orEmpty() },
                { it.sortOrder },
                { it.id },
            ),
        )
    }

    /** A package is applicable when it is non-empty, intact and structurally consistent. */
    fun validate(packageDto: ContentPackageDto): ValidationResult {
        if (packageDto.nodes.isEmpty()) {
            return ValidationResult(false, "Package contained no content")
        }
        if (!ContentChecksum.isIntact(packageDto)) {
            return ValidationResult(false, "Checksum mismatch")
        }
        val requireParents = packageDto.fullSnapshot
        if (orderParentsFirst(packageDto.nodes, requireParents) == null) {
            return ValidationResult(false, "Malformed package: inconsistent parent references")
        }
        return ValidationResult(true, null)
    }

    private fun depthOf(node: ContentNodeDto, byId: Map<String, ContentNodeDto>): Int {
        var depth = 0
        var parentId = node.parentId
        var guard = 0
        while (parentId != null && guard++ < MAX_DEPTH) {
            depth++
            parentId = byId[parentId]?.parentId
        }
        return depth
    }

    private const val MAX_DEPTH = 512
}

data class ValidationResult(val valid: Boolean, val reason: String?)
