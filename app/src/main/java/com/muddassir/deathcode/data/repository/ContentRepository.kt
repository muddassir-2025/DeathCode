package com.muddassir.deathcode.data.repository

import androidx.room.withTransaction
import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.core.newId
import com.muddassir.deathcode.core.slugify
import com.muddassir.deathcode.data.local.db.DeathCodeDatabase
import com.muddassir.deathcode.data.local.db.dao.AncestryRow
import com.muddassir.deathcode.data.local.db.dao.ContentNodeDao
import com.muddassir.deathcode.data.local.db.dao.KeyboardKeywordDao
import com.muddassir.deathcode.data.local.db.dao.PathRow
import com.muddassir.deathcode.data.local.db.dao.PrivateOverrideDao
import com.muddassir.deathcode.data.local.db.dao.SearchDao
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.KeyboardKeywordEntity
import com.muddassir.deathcode.data.local.search.SearchIndexer
import com.muddassir.deathcode.domain.model.ContentSource
import com.muddassir.deathcode.markdown.HierarchyNode
import com.muddassir.deathcode.markdown.MarkdownTreeParser
import com.muddassir.deathcode.markdown.SectionKind
import kotlinx.coroutines.flow.Flow

/** Outcome of a markdown import, used by the preview/save UI. */
data class MarkdownImportResult(
    val createdNodeIds: List<String>,
    val warnings: List<String>,
    val headingCount: Int,
)

/** Fields accepted when creating or editing a content card. */
data class ContentDraft(
    val title: String,
    val markdown: String = "",
    val syntax: String? = null,
    val notes: String? = null,
    val language: String? = null,
    val keywords: List<String> = emptyList(),
)

/**
 * All reads and writes of the recursive content tree.
 *
 * Every write keeps three things consistent inside one transaction:
 * 1. `content_nodes` (the tree itself)
 * 2. `node_fts` (the offline search index)
 * 3. `keyboard_keywords` (the offline keyboard index)
 *
 * Nothing here touches the network: the tree is the runtime source of truth.
 */
class ContentRepository(
    private val db: DeathCodeDatabase,
    private val nodeDao: ContentNodeDao,
    private val searchDao: SearchDao,
    private val overrideDao: PrivateOverrideDao,
    private val keywordDao: KeyboardKeywordDao,
) {

    // ---------------------------------------------------------------- reads

    fun observeChildren(parentId: String?): Flow<List<ContentNodeEntity>> =
        nodeDao.observeChildren(parentId)

    fun observeRoots(source: ContentSource): Flow<List<ContentNodeEntity>> =
        nodeDao.observeRoots(source)

    fun observeNode(id: String): Flow<ContentNodeEntity?> = nodeDao.observeById(id)

    suspend fun getNode(id: String): ContentNodeEntity? = nodeDao.getById(id)

    suspend fun getChildren(parentId: String?): List<ContentNodeEntity> =
        nodeDao.getChildren(parentId)

    /** Ancestors from the root down to (and including) [id]. */
    suspend fun getPath(id: String): List<PathRow> = nodeDao.getPath(id)

    suspend fun getSubtree(id: String): List<ContentNodeEntity> = nodeDao.getSubtree(id)

    suspend fun hasChildren(id: String): Boolean = nodeDao.countChildren(id) > 0

    suspend fun getRoots(source: ContentSource): List<ContentNodeEntity> = nodeDao.getRoots(source)

    suspend fun getNodesBySource(source: ContentSource): List<ContentNodeEntity> =
        nodeDao.getAllBySource(source)

    suspend fun getAncestryRows(): List<AncestryRow> = nodeDao.getAncestryRows()

    /**
     * Builds the continuous document used by Reading Mode.
     *
     * The node's own markdown is followed by its whole subtree in tree order, with each
     * title promoted to a heading — the same content that can be browsed card by card can
     * therefore also be read as one document (§17).
     */
    suspend fun readingDocument(rootId: String): String {
        val subtree = nodeDao.getSubtree(rootId)
        if (subtree.isEmpty()) return ""
        val root = subtree.firstOrNull { it.id == rootId } ?: return ""
        val byParent = subtree.groupBy { it.parentId }
        val builder = StringBuilder()

        fun walk(node: ContentNodeEntity, depth: Int) {
            builder.append("#".repeat(depth.coerceIn(1, 6)))
                .append(' ')
                .append(node.title)
                .append("\n\n")
            if (node.markdown.isNotBlank()) {
                builder.append(node.markdown.trim()).append("\n\n")
            }
            node.syntax?.takeIf { it.isNotBlank() }?.let { syntax ->
                builder.append("```").append(node.language.orEmpty()).append('\n')
                    .append(syntax.trim()).append("\n```\n\n")
            }
            node.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                builder.append("> ").append(notes.trim().replace("\n", "\n> ")).append("\n\n")
            }
            byParent[node.id].orEmpty()
                .sortedWith(compareBy({ it.sortOrder }, { it.title }))
                .forEach { child -> walk(child, depth + 1) }
        }

        walk(root, 1)
        return builder.toString().trim()
    }

    // --------------------------------------------------------------- writes

    suspend fun createNode(
        parentId: String?,
        draft: ContentDraft,
        source: ContentSource,
        ownerId: String? = null,
    ): String {
        val now = System.currentTimeMillis()
        val id = newId("node")
        val pathTitles = parentId?.let { nodeDao.getPath(it).map { row -> row.title } } ?: emptyList()

        db.withTransaction {
            val order = (nodeDao.maxSortOrder(parentId) ?: -1) + 1
            val node = ContentNodeEntity(
                id = id,
                parentId = parentId,
                title = draft.title.trim().ifEmpty { "Untitled" },
                slug = slugify(draft.title),
                markdown = draft.markdown,
                syntax = draft.syntax,
                notes = draft.notes,
                language = draft.language,
                keywords = KeywordList.serialize(draft.keywords),
                source = source,
                ownerId = ownerId,
                sortOrder = order,
                createdAt = now,
                updatedAt = now,
            )
            nodeDao.insert(node)
            writeIndexes(node, pathTitles + node.title, override = null)
        }
        return id
    }

    suspend fun updateNode(
        id: String,
        markdown: String,
        syntax: String?,
        notes: String?,
        language: String?,
        keywords: List<String>,
    ) {
        val existing = nodeDao.getById(id) ?: return
        val now = System.currentTimeMillis()
        db.withTransaction {
            nodeDao.update(
                existing.copy(
                    markdown = markdown,
                    syntax = syntax,
                    notes = notes,
                    language = language,
                    keywords = KeywordList.serialize(keywords),
                    updatedAt = now,
                ),
            )
            writeIndexes(
                nodeDao.getById(id)!!,
                pathTitlesOf(id),
                overrideDao.getForNode(id),
            )
        }
    }

    suspend fun renameNode(id: String, title: String) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            nodeDao.rename(id, title.trim().ifEmpty { "Untitled" }, slugify(title), now)
            reindexSubtree(id)
        }
    }

    /** Moves [id] under [newParentId] (null == root). Rejects cycles. */
    suspend fun moveNode(id: String, newParentId: String?): Boolean {
        if (id == newParentId) return false
        if (newParentId != null) {
            val subtreeIds = nodeDao.getSubtreeIds(id)
            if (newParentId in subtreeIds) return false
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val order = (nodeDao.maxSortOrder(newParentId) ?: -1) + 1
            nodeDao.updateParent(id, newParentId, order, now)
            reindexSubtree(id)
        }
        return true
    }

    suspend fun reorderSiblings(parentId: String?, orderedIds: List<String>) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            orderedIds.forEachIndexed { index, id ->
                nodeDao.updateParent(id, parentId, index, now)
            }
            orderedIds.forEach { reindexSubtree(it) }
        }
    }

    /** Deletes the node and, via the self-referencing cascade, its whole subtree. */
    suspend fun deleteNode(id: String) {
        val subtreeIds = nodeDao.getSubtreeIds(id)
        val now = System.currentTimeMillis()
        db.withTransaction {
            nodeDao.deleteById(id)
            if (subtreeIds.isNotEmpty()) {
                searchDao.deleteByNodeIds(subtreeIds)
                keywordDao.deleteForTargets(KeyboardKeywordEntity.TARGET_NODE, subtreeIds)
            }
        }
    }

    suspend fun duplicateNode(id: String, targetParentId: String?): String? {
        val source = nodeDao.getById(id) ?: return null
        val subtree = nodeDao.getSubtree(id)
        val byParent = subtree.groupBy { it.parentId }
        val now = System.currentTimeMillis()

        var newRootId: String? = null
        db.withTransaction {
            val baseOrder = (nodeDao.maxSortOrder(targetParentId) ?: -1) + 1
            val order = baseOrder

            suspend fun copyNode(node: ContentNodeEntity, parentId: String?, sortOrder: Int): String {
                val copyId = newId("node")
                val copyPath = nodeDao.getPath(node.id).map { it.title }
                val copy = node.copy(
                    id = copyId,
                    parentId = parentId,
                    sortOrder = sortOrder,
                    createdAt = now,
                    updatedAt = now,
                    // A duplicate is always private material, even when the source was
                    // official/community content.
                    source = ContentSource.PRIVATE,
                    ownerId = null,
                    remoteId = null,
                )
                nodeDao.insert(copy)
                writeIndexes(copy, copyPath, override = null)
                byParent[node.id].orEmpty().forEachIndexed { index, child ->
                    copyNode(child, copyId, index)
                }
                return copyId
            }

            newRootId = copyNode(source, targetParentId, order)
        }
        return newRootId
    }

    /**
     * Imports markdown **relative to [parentNodeId]**.
     *
     * Heading levels are interpreted relative to the imported document, so the same
     * document can be imported at any depth. Leaf `Syntax`, `Keywords` and `Notes`
     * sections are folded into the parent card instead of becoming extra categories.
     */
    suspend fun importMarkdown(
        parentNodeId: String?,
        markdown: String,
        source: ContentSource = ContentSource.PRIVATE,
        ownerId: String? = null,
    ): MarkdownImportResult {
        val parsed = MarkdownTreeParser.parse(markdown)
        val created = mutableListOf<String>()
        val now = System.currentTimeMillis()

        db.withTransaction {
            val basePath = parentNodeId?.let { nodeDao.getPath(it).map { row -> row.title } }
                ?: emptyList()

            if (parsed.roots.isEmpty()) {
                // A heading-less document becomes one card under the current node.
                val title = markdown.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() }?.take(80)
                    ?: "Imported content"
                val id = newId("node")
                val order = (nodeDao.maxSortOrder(parentNodeId) ?: -1) + 1
                val node = ContentNodeEntity(
                    id = id,
                    parentId = parentNodeId,
                    title = title,
                    slug = slugify(title),
                    markdown = markdown,
                    source = source,
                    ownerId = ownerId,
                    sortOrder = order,
                    createdAt = now,
                    updatedAt = now,
                )
                nodeDao.insert(node)
                writeIndexes(node, basePath + title, override = null)
                created += id
            } else {
                var nextOrder = (nodeDao.maxSortOrder(parentNodeId) ?: -1) + 1
                parsed.roots.forEach { root ->
                    val id = persistHierarchy(
                        node = root,
                        parentId = parentNodeId,
                        parentPath = basePath,
                        source = source,
                        ownerId = ownerId,
                        sortOrder = nextOrder++,
                        created = created,
                    )
                    created += id
                }
            }
        }

        return MarkdownImportResult(
            createdNodeIds = created,
            warnings = parsed.warnings,
            headingCount = parsed.headingCount,
        )
    }

    /** Creates (once) and returns the private `My Notes` root. */
    suspend fun ensurePrivateRoot(): String {
        nodeDao.getRoots(ContentSource.PRIVATE)
            .firstOrNull { it.title.equals(PRIVATE_ROOT_TITLE, ignoreCase = true) }
            ?.let { return it.id }
        return createNode(
            parentId = null,
            draft = ContentDraft(title = PRIVATE_ROOT_TITLE),
            source = ContentSource.PRIVATE,
        )
    }

    // ------------------------------------------------------------- indexing

    /** Rebuilds the FTS + keyboard indexes for a single node. */
    suspend fun reindex(nodeId: String) {
        val node = nodeDao.getById(nodeId) ?: return
        db.withTransaction {
            writeIndexes(node, pathTitlesOf(nodeId), overrideDao.getForNode(nodeId))
        }
    }

    /** Rebuilds indexes for [nodeId] and every descendant (needed after rename/move). */
    suspend fun reindexSubtree(nodeId: String) {
        nodeDao.getSubtreeIds(nodeId).forEach { id ->
            val node = nodeDao.getById(id) ?: return@forEach
            writeIndexes(node, pathTitlesOf(id), overrideDao.getForNode(id))
        }
    }

    /** Rebuilds every index; used after a content synchronization or a bulk import. */
    suspend fun reindexAll() {
        searchDao.clear()
        nodeDao.getAll().forEach { node ->
            writeIndexes(node, pathTitlesOf(node.id), overrideDao.getForNode(node.id))
        }
    }

    private suspend fun pathTitlesOf(nodeId: String): List<String> =
        nodeDao.getPath(nodeId).map { row -> row.title }

    private suspend fun writeIndexes(
        node: ContentNodeEntity,
        pathTitles: List<String>,
        override: com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity?,
    ) {
        searchDao.deleteByNodeId(node.id)
        searchDao.upsert(SearchIndexer.document(node, pathTitles, override))

        val keywords = (
            KeywordList.parse(node.keywords) + KeywordList.parse(override?.keywords)
            ).distinct()
        keywordDao.deleteForTarget(KeyboardKeywordEntity.TARGET_NODE, node.id)
        if (keywords.isNotEmpty()) {
            keywordDao.upsertAll(
                keywords.map { keyword ->
                    KeyboardKeywordEntity(
                        id = "kw-node-${node.id}-$keyword",
                        keyword = keyword,
                        targetType = KeyboardKeywordEntity.TARGET_NODE,
                        targetId = node.id,
                        language = node.language.orEmpty(),
                        source = node.source,
                        updatedAt = node.updatedAt,
                    )
                },
            )
        }
    }

    // ------------------------------------------------------- import helpers

    private suspend fun persistHierarchy(
        node: HierarchyNode,
        parentId: String?,
        parentPath: List<String>,
        source: ContentSource,
        ownerId: String?,
        sortOrder: Int,
        created: MutableList<String>,
    ): String {
        val id = newId("node")
        val title = node.title.trim().ifEmpty { "Untitled" }

        // Fold well-known leaf sections into the card instead of creating categories.
        var syntax: String? = null
        var language: String? = null
        var notes: String? = null
        var keywords: String = ""
        val realChildren = mutableListOf<HierarchyNode>()

        node.children.forEach { child ->
            if (child.isLeaf && child.sectionKind != SectionKind.NONE) {
                when (child.sectionKind) {
                    SectionKind.KEYWORDS -> keywords = keywordsFromSection(child.content)
                    SectionKind.SYNTAX -> {
                        val extracted = extractCode(child.content)
                        syntax = extracted.second
                        language = extracted.first ?: language
                    }
                    SectionKind.NOTES -> notes = child.content
                    SectionKind.NONE -> realChildren += child
                }
            } else {
                realChildren += child
            }
        }

        val now = System.currentTimeMillis()
        val entity = ContentNodeEntity(
            id = id,
            parentId = parentId,
            title = title,
            slug = slugify(title),
            markdown = node.content,
            syntax = syntax,
            notes = notes,
            language = language,
            keywords = keywords,
            source = source,
            ownerId = ownerId,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now,
        )
        nodeDao.insert(entity)
        writeIndexes(entity, parentPath + title, override = null)

        val childPath = parentPath + title
        realChildren.forEachIndexed { index, child ->
            val childId = persistHierarchy(
                node = child,
                parentId = id,
                parentPath = childPath,
                source = source,
                ownerId = ownerId,
                sortOrder = index,
                created = created,
            )
            created += childId
        }
        return id
    }

    private fun keywordsFromSection(content: String): String {
        val words = content.lineSequence()
            .map { line ->
                line.trim()
                    .removePrefix("-").removePrefix("*").removePrefix("+")
                    .trim()
                    .trim('`')
            }
            .filter { it.isNotEmpty() }
            .flatMap { it.split(',', ';') }
            .map { it.trim() }
            .toList()
        return KeywordList.serialize(words)
    }

    /** Extracts `(language, code)` from the first fenced block, else `(null, content)`. */
    private fun extractCode(content: String): Pair<String?, String> {
        val fence = Regex("""^\s*`{3,}\s*([\w+#.-]*)\s*$""")
        val lines = content.lines()
        val openIndex = lines.indexOfFirst { fence.matches(it) }
        if (openIndex < 0) return null to content.trim()
        val language = fence.find(lines[openIndex])?.groupValues?.get(1)?.ifBlank { null }
        val closingIndex = lines.drop(openIndex + 1).indexOfFirst { it.trim().startsWith("```") }
        if (closingIndex < 0) return language to content.trim()
        val code = lines.subList(openIndex + 1, openIndex + 1 + closingIndex).joinToString("\n")
        return language to code.trim('\n')
    }

    companion object {
        const val PRIVATE_ROOT_TITLE = "My Notes"
    }
}
