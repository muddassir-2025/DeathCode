package com.muddassir.deathcode.data.local.search

import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.NodeFtsEntity
import com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity

/**
 * Builds the searchable document for a node.
 *
 * The private override is folded into the same document, which is what makes the user's
 * personal notes, personal templates and personal keywords searchable **offline** through
 * exactly the same pipeline as official content.
 */
object SearchIndexer {

    fun document(
        node: ContentNodeEntity,
        pathTitles: List<String>,
        override: PrivateOverrideEntity? = null,
    ): NodeFtsEntity = NodeFtsEntity(
        nodeId = node.id,
        title = node.title,
        path = pathTitles.joinToString(" "),
        markdown = node.markdown,
        syntax = listOfNotNull(override?.syntax, override?.template)
            .joinToString("\n")
            .ifBlank { node.syntax.orEmpty() },
        notes = listOfNotNull(node.notes, override?.note).joinToString("\n"),
        keywords = listOfNotNull(node.keywords, override?.keywords)
            .filter { it.isNotBlank() }
            .joinToString(" "),
    )
}
