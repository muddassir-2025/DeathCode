package com.muddassir.deathcode.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.data.local.db.dao.AncestryRow
import com.muddassir.deathcode.data.local.db.dao.PathRow
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.local.db.entity.PrivateOverrideEntity
import com.muddassir.deathcode.data.repository.ContentDraft
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How a container is displayed: tappable child cards, or a continuous document. */
enum class ViewMode { CARDS, READING }

data class MoveTarget(
    val id: String?,
    val title: String,
    val path: String,
)

data class BrowseState(
    val node: ContentNodeEntity? = null,
    val children: List<ContentNodeEntity> = emptyList(),
    val path: List<PathRow> = emptyList(),
    val mode: ViewMode = ViewMode.CARDS,
    val readingDocument: String = "",
    val override: PrivateOverrideEntity? = null,
    val adminMode: Boolean = false,
) {
    val hasChildren: Boolean get() = children.isNotEmpty()
    val hasContent: Boolean
        get() = node?.let {
            it.markdown.isNotBlank() || !it.syntax.isNullOrBlank() ||
                !it.notes.isNullOrBlank() || it.keywords.isNotBlank()
        } == true

    val keywords: List<String> get() = KeywordList.parse(node?.keywords) +
        KeywordList.parse(override?.keywords)

    /**
     * Official and community content is read-only for normal users; only private content is
     * editable. In admin mode official content becomes editable as well.
     */
    val isEditable: Boolean
        get() = node?.source == ContentSource.PRIVATE ||
            (adminMode && node?.source == ContentSource.OFFICIAL)

    val canWriteContent: Boolean get() = node != null
}

/**
 * Backs one browser destination.
 *
 * The same screen serves official content, the user's private tree and synchronized
 * community content; only the write affordances change based on the node's source.
 */
class BrowseViewModel(
    private val container: AppContainer,
    private val nodeId: String,
    /** Admin mode allows editing official content, mirroring the server side permission. */
    private val adminMode: Boolean = false,
) : ViewModel() {

    private val mode = MutableStateFlow(ViewMode.CARDS)
    private val path = MutableStateFlow<List<PathRow>>(emptyList())
    private val readingDocument = MutableStateFlow("")
    private val override = MutableStateFlow<PrivateOverrideEntity?>(null)

    val state: StateFlow<BrowseState> = combine(
        container.contentRepository.observeNode(nodeId),
        container.contentRepository.observeChildren(nodeId),
        mode,
        path,
        readingDocument,
        override,
    ) { values ->
        BrowseState(
            node = values[0] as ContentNodeEntity?,
            children = values[1] as List<ContentNodeEntity>,
            mode = values[2] as ViewMode,
            path = values[3] as List<PathRow>,
            readingDocument = values[4] as String,
            override = values[5] as PrivateOverrideEntity?,
            adminMode = adminMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseState())

    init {
        viewModelScope.launch { refresh() }
    }

    fun setMode(value: ViewMode) {
        mode.value = value
        if (value == ViewMode.READING) {
            viewModelScope.launch { rebuildReadingDocument() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            path.value = container.contentRepository.getPath(nodeId)
            override.value = container.personalRepository.get(nodeId)
            rebuildReadingDocument()
        }
    }

    // ------------------------------------------------------------- mutations

    /**
     * Manual creation at any depth: the new node is a child of the currently open node.
     */
    fun createChild(title: String, markdown: String = "") {
        val source = currentSource()
        viewModelScope.launch {
            container.contentRepository.createNode(
                parentId = nodeId,
                draft = ContentDraft(title = title, markdown = markdown),
                source = source,
            )
            refresh()
        }
    }

    /** Markdown creation at any depth, interpreted relative to the current node. */
    fun importMarkdown(markdown: String) {
        val source = currentSource()
        viewModelScope.launch {
            container.contentRepository.importMarkdown(nodeId, markdown, source)
            refresh()
        }
    }

    fun updateContent(
        markdown: String,
        syntax: String?,
        notes: String?,
        language: String?,
        keywords: List<String>,
    ) {
        viewModelScope.launch {
            container.contentRepository.updateNode(
                id = nodeId,
                markdown = markdown,
                syntax = syntax,
                notes = notes,
                language = language,
                keywords = keywords,
            )
            refresh()
        }
    }

    fun rename(title: String) {
        viewModelScope.launch {
            container.contentRepository.renameNode(nodeId, title)
            refresh()
        }
    }

    fun delete() {
        viewModelScope.launch { container.contentRepository.deleteNode(nodeId) }
    }

    fun duplicate() {
        viewModelScope.launch {
            val node = container.contentRepository.getNode(nodeId) ?: return@launch
            container.contentRepository.duplicateNode(nodeId, node.parentId)
            refresh()
        }
    }

    fun moveTo(parentId: String?) {
        viewModelScope.launch {
            container.contentRepository.moveNode(nodeId, parentId)
            refresh()
        }
    }

    fun saveOverride(
        note: String,
        template: String?,
        syntax: String?,
        keywords: List<String>,
        language: String?,
    ) {
        viewModelScope.launch {
            container.personalRepository.save(nodeId, note, template, syntax, keywords, language)
            refresh()
        }
    }

    fun clearOverride() {
        viewModelScope.launch {
            container.personalRepository.clear(nodeId)
            refresh()
        }
    }

    /** Admin: publish the locally edited official content as a new version. */
    fun publishOfficial() {
        viewModelScope.launch { container.syncRepository.publishOfficialDraft() }
    }

    /** Candidate parents for the move dialog: excludes the node itself and its subtree. */
    suspend fun moveTargets(): List<MoveTarget> {
        val current = state.value.node ?: return emptyList()
        val subtreeIds = container.contentRepository.getSubtree(nodeId).map { it.id }.toSet()
        val targets = mutableListOf(MoveTarget(id = null, title = "Top level", path = ""))
        val privateNodes = container.contentRepository.getNodesBySource(ContentSource.PRIVATE)
        val ancestry = container.contentRepository.getAncestryRows().associateBy { it.id }
        privateNodes
            .filter { it.id !in subtreeIds && it.id != current.id }
            .forEach { node ->
                targets += MoveTarget(
                    id = node.id,
                    title = node.title,
                    path = breadcrumbOf(node.id, ancestry),
                )
            }
        return targets
    }

    private fun breadcrumbOf(nodeId: String, ancestry: Map<String, AncestryRow>): String {
        val parts = ArrayDeque<String>()
        var current = ancestry[nodeId]
        var guard = 0
        while (current != null && guard++ < 200) {
            parts.addFirst(current.title)
            current = current.parentId?.let { ancestry[it] }
        }
        return parts.joinToString(" > ")
    }

    // ---------------------------------------------------------------- helpers

    private fun currentSource(): ContentSource =
        state.value.node?.source ?: ContentSource.PRIVATE

    private suspend fun rebuildReadingDocument() {
        readingDocument.value = container.contentRepository.readingDocument(nodeId)
    }
}
