package com.muddassir.deathcode.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.markdown.MarkdownTreeParser
import com.muddassir.deathcode.syntax.Languages
import com.muddassir.deathcode.ui.components.Breadcrumb
import com.muddassir.deathcode.ui.components.CodeBlock
import com.muddassir.deathcode.ui.components.ConfirmDialog
import com.muddassir.deathcode.ui.components.EmptyState
import com.muddassir.deathcode.ui.components.KeywordChips
import com.muddassir.deathcode.ui.components.MarkdownView
import com.muddassir.deathcode.ui.components.SourceBadge
import com.muddassir.deathcode.ui.rememberAppViewModel

/**
 * A recursive browser destination.
 *
 * Every node — official, private or community — is browsed by the same screen. It supports
 * unlimited nesting, a Cards/Reading toggle, private personalization of read-only content,
 * and (at any depth) manual creation or markdown import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    nodeId: String,
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    showBack: Boolean = true,
    adminMode: Boolean = false,
) {
    val viewModel: BrowseViewModel = rememberAppViewModel(key = "browse-$nodeId") {
        BrowseViewModel(it, nodeId, adminMode = adminMode)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showOverrideDialog by remember { mutableStateOf(false) }
    var showContentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.node?.title ?: "Content") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (state.node != null) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (adminMode) {
                                DropdownMenuItem(
                                    text = { Text("Publish new version") },
                                    onClick = { showMenu = false; viewModel.publishOfficial() },
                                )
                            }
                            if (state.isEditable) {
                                DropdownMenuItem(
                                    text = { Text("Edit content") },
                                    onClick = { showMenu = false; showContentDialog = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = { showMenu = false; showRenameDialog = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to...") },
                                    onClick = { showMenu = false; showMoveDialog = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    onClick = { showMenu = false; viewModel.duplicate() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = { showMenu = false; showDeleteDialog = true },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Duplicate to My Notes") },
                                    onClick = { showMenu = false; viewModel.duplicate() },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.node != null) {
                Box {
                    FloatingActionButton(onClick = { showAddMenu = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add content")
                    }
                    DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Create manually") },
                            onClick = { showAddMenu = false; showCreateDialog = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Create using Markdown") },
                            onClick = { showAddMenu = false; showImportDialog = true },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Breadcrumb(path = state.path)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.node?.let { SourceBadge(it.source) }
                FilterChip(
                    selected = state.mode == ViewMode.CARDS,
                    onClick = { viewModel.setMode(ViewMode.CARDS) },
                    label = { Text("Cards") },
                )
                FilterChip(
                    selected = state.mode == ViewMode.READING,
                    onClick = { viewModel.setMode(ViewMode.READING) },
                    label = { Text("Reading") },
                )
            }

            if (state.mode == ViewMode.CARDS) {
                ContentSection(state = state, onEdit = { showOverrideDialog = true })
                ChildCards(state = state, onOpenNode = onOpenNode)
            } else {
                if (state.readingDocument.isBlank()) {
                    EmptyState(
                        title = "Nothing to read yet",
                        message = "Add markdown or create subcategories to build this document.",
                    )
                } else {
                    MarkdownView(markdown = state.readingDocument)
                }
            }

            Box(modifier = Modifier.padding(bottom = 88.dp))
        }
    }

    if (showCreateDialog) {
        CreateNodeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, markdown ->
                viewModel.createChild(title, markdown)
                showCreateDialog = false
            },
        )
    }

    if (showImportDialog) {
        MarkdownImportDialog(
            nodeTitle = state.node?.title.orEmpty(),
            onDismiss = { showImportDialog = false },
            onImport = { markdown ->
                viewModel.importMarkdown(markdown)
                showImportDialog = false
            },
        )
    }

    if (showRenameDialog) {
        TextInputDialog(
            title = "Rename",
            label = "Title",
            initial = state.node?.title.orEmpty(),
            onDismiss = { showRenameDialog = false },
            onConfirm = { viewModel.rename(it); showRenameDialog = false },
        )
    }

    if (showContentDialog) {
        ContentEditorDialog(
            initialMarkdown = state.node?.markdown.orEmpty(),
            initialSyntax = state.node?.syntax.orEmpty(),
            initialNotes = state.node?.notes.orEmpty(),
            initialLanguage = state.node?.language,
            initialKeywords = KeywordList.parse(state.node?.keywords),
            onDismiss = { showContentDialog = false },
            onSave = { markdown, syntax, notes, language, keywords ->
                viewModel.updateContent(markdown, syntax, notes, language, keywords)
                showContentDialog = false
            },
        )
    }

    if (showOverrideDialog && state.node != null) {
        ContentEditorDialog(
            title = "My private note",
            confirmLabel = "Save",
            initialMarkdown = state.override?.note.orEmpty(),
            markdownLabel = "My note",
            initialSyntax = state.override?.syntax.orEmpty(),
            syntaxLabel = "My syntax",
            initialNotes = state.override?.template.orEmpty(),
            notesLabel = "My template",
            initialLanguage = state.override?.language,
            initialKeywords = KeywordList.parse(state.override?.keywords),
            onDismiss = { showOverrideDialog = false },
            showClear = state.override != null,
            onClear = { viewModel.clearOverride(); showOverrideDialog = false },
            onSave = { note, syntax, template, language, keywords ->
                viewModel.saveOverride(note, template, syntax, keywords, language)
                showOverrideDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete content",
            message = "This deletes the node and everything nested inside it. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.delete(); showDeleteDialog = false; onBack() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showMoveDialog) {
        MoveDialog(
            viewModel = viewModel,
            onDismiss = { showMoveDialog = false },
            onMoved = { parentId -> viewModel.moveTo(parentId); showMoveDialog = false },
        )
    }
}

@Composable
private fun ContentSection(state: BrowseState, onEdit: () -> Unit) {
    val node = state.node ?: return

    if (state.hasContent) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (node.markdown.isNotBlank()) {
                    MarkdownView(markdown = node.markdown)
                }
                node.syntax?.takeIf { it.isNotBlank() }?.let { syntax ->
                    CodeBlock(code = syntax, language = node.language)
                }
                node.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    MarkdownView(markdown = notes)
                }
                val keywords = KeywordList.parse(node.keywords)
                if (keywords.isNotEmpty()) {
                    KeywordChips(keywords = keywords)
                }
            }
        }
    }

    val override = state.override
    if (override != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("My private note", style = MaterialTheme.typography.titleMedium)
                if (override.note.isNotBlank()) {
                    Text(override.note, style = MaterialTheme.typography.bodyLarge)
                }
                override.template?.takeIf { it.isNotBlank() }?.let { template ->
                    CodeBlock(code = template, language = override.language ?: node.language)
                }
                val keywords = KeywordList.parse(override.keywords)
                if (keywords.isNotEmpty()) KeywordChips(keywords = keywords)
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Text(
                text = if (override == null) "Add private note" else "Edit private note",
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun ChildCards(state: BrowseState, onOpenNode: (String) -> Unit) {
    if (!state.hasChildren) {
        if (!state.hasContent) {
            EmptyState(
                title = "This node is empty",
                message = "Create a subcategory, add content manually, or import Markdown.",
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.heightIn(max = 2000.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        items(state.children, key = { it.id }) { child ->
            Card(
                onClick = { onOpenNode(child.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(child.title, style = MaterialTheme.typography.titleMedium)
                        SourceBadge(child.source)
                    }
                    val preview = child.markdown.lineSequence()
                        .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                    if (preview != null) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
