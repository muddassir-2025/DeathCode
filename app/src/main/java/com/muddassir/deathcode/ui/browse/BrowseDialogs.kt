package com.muddassir.deathcode.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.muddassir.deathcode.markdown.HierarchyNode
import com.muddassir.deathcode.markdown.MarkdownTreeParser
import com.muddassir.deathcode.syntax.Languages
import com.muddassir.deathcode.ui.components.MonoText

/** Simple single-field prompt. */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String = "Save",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Manual creation: a title plus optional starting markdown. */
@Composable
fun CreateNodeDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, markdown: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var markdown by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create manually") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = markdown,
                    onValueChange = { markdown = it },
                    label = { Text("Content (markdown, optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title, markdown) },
                enabled = title.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Markdown editor with a live hierarchy preview.
 *
 * The preview shows exactly what will be created *relative to the current node*, so the
 * user can verify the structure before committing (§10).
 */
@Composable
fun MarkdownImportDialog(
    nodeTitle: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var markdown by remember { mutableStateOf("") }
    val parsed = remember(markdown) { MarkdownTreeParser.parse(markdown) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create using Markdown") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = markdown,
                    onValueChange = { markdown = it },
                    label = { Text("Markdown") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                )

                Text("Preview", style = MaterialTheme.typography.titleMedium)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        MonoText(
                            text = if (nodeTitle.isBlank()) "Top level" else nodeTitle,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        parsed.roots.forEach { root -> PreviewNode(root, depth = 1) }
                        if (parsed.isEmpty && markdown.isNotBlank()) {
                            Text(
                                text = "No headings found — this will be saved as a single card.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                parsed.warnings.forEach { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(markdown) },
                enabled = markdown.isNotBlank(),
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PreviewNode(node: HierarchyNode, depth: Int) {
    Column(modifier = Modifier.padding(start = (depth * 14).dp, top = 4.dp)) {
        MonoText(text = "${"·".repeat(0)}${node.title}")
        node.children.forEach { child -> PreviewNode(child, depth + 1) }
    }
}

/**
 * Editor for a content card. The same dialog edits official/private content fields and the
 * user's private personalization by relabelling the fields.
 */
@Composable
fun ContentEditorDialog(
    onDismiss: () -> Unit,
    onSave: (markdown: String, syntax: String?, notes: String?, language: String?, keywords: List<String>) -> Unit,
    title: String = "Edit content",
    confirmLabel: String = "Save",
    initialMarkdown: String = "",
    initialSyntax: String = "",
    initialNotes: String = "",
    initialLanguage: String? = null,
    initialKeywords: List<String> = emptyList(),
    markdownLabel: String = "Markdown",
    syntaxLabel: String = "Syntax",
    notesLabel: String = "Notes",
    showClear: Boolean = false,
    onClear: () -> Unit = {},
) {
    var markdown by remember { mutableStateOf(initialMarkdown) }
    var syntax by remember { mutableStateOf(initialSyntax) }
    var notes by remember { mutableStateOf(initialNotes) }
    var keywords by remember { mutableStateOf(initialKeywords.joinToString(", ")) }
    var language by remember { mutableStateOf(initialLanguage ?: Languages.cpp.id) }
    var languageMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = markdown,
                    onValueChange = { markdown = it },
                    label = { Text(markdownLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                )
                OutlinedTextField(
                    value = syntax,
                    onValueChange = { syntax = it },
                    label = { Text(syntaxLabel) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(notesLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp),
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keyboard keywords (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    OutlinedButton(onClick = { languageMenu = true }) {
                        Text(Languages.displayName(language))
                    }
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        Languages.pickerOptions.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { language = id; languageMenu = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (showClear) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
                TextButton(
                    onClick = {
                        onSave(
                            markdown,
                            syntax.ifBlank { null },
                            notes.ifBlank { null },
                            language,
                            keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                        )
                    },
                ) { Text(confirmLabel) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Picks a new parent node for a private node. */
@Composable
fun MoveDialog(
    viewModel: BrowseViewModel,
    onDismiss: () -> Unit,
    onMoved: (String?) -> Unit,
) {
    var targets by remember { mutableStateOf<List<MoveTarget>>(emptyList()) }
    LaunchedEffect(Unit) { targets = viewModel.moveTargets() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(targets) { target ->
                    Card(
                        onClick = { onMoved(target.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(target.title, style = MaterialTheme.typography.titleMedium)
                            if (target.path.isNotBlank()) {
                                Text(
                                    text = target.path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
