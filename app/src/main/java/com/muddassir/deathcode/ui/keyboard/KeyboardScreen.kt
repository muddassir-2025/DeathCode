package com.muddassir.deathcode.ui.keyboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muddassir.deathcode.data.local.db.entity.SnippetEntity
import com.muddassir.deathcode.syntax.Languages
import com.muddassir.deathcode.ui.browse.ContentEditorDialog
import com.muddassir.deathcode.ui.components.EmptyState
import com.muddassir.deathcode.ui.rememberAppViewModel

/**
 * Keyboard hub: enablement instructions, keyboard preferences and snippet management.
 *
 * All preferences are stored locally and are shared with the IME service, so a change here
 * takes effect in the keyboard immediately.
 */
@Composable
fun KeyboardScreen() {
    val viewModel: KeyboardViewModel = rememberAppViewModel { KeyboardViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editing by remember { mutableStateOf<SnippetEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New snippet")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Death Code Keyboard", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "A programming keyboard that suggests your own snippets " +
                                "and templates while you type — entirely offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { openKeyboardSettings(context) }) {
                                Text("Enable keyboard")
                            }
                            OutlinedButton(onClick = { showKeyboardPicker(context) }) {
                                Text("Choose keyboard")
                            }
                        }
                        Text(
                            text = "1. Enable \"Death Code\" in system settings.\n" +
                                "2. Pick it from the keyboard switcher in any text field.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SettingsCard(title = "Layout") {
                    ToggleRow(
                        label = "Suggestion row",
                        checked = state.settings.showSuggestions,
                        onChange = viewModel::setShowSuggestions,
                    )
                    ToggleRow(
                        label = "Programming symbol row",
                        checked = state.settings.showSymbolRow,
                        onChange = viewModel::setShowSymbolRow,
                    )
                    Text(
                        text = "Key height: ${state.settings.keyHeightDp}dp",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = state.settings.keyHeightDp.toFloat(),
                        onValueChange = { viewModel.setKeyHeight(it.toInt()) },
                        valueRange = 40f..76f,
                    )
                    ToggleRow(
                        label = "Haptics",
                        checked = state.settings.hapticsEnabled,
                        onChange = viewModel::setHaptics,
                    )
                    ToggleRow(
                        label = "Sound",
                        checked = state.settings.soundEnabled,
                        onChange = viewModel::setSound,
                    )
                }
            }

            item {
                SettingsCard(title = "Keyboard language") {
                    Text(
                        text = "Templates for ${Languages.displayName(state.settings.keyboardLanguage)} " +
                            "are preferred when several languages define the same keyword.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LanguagePicker(
                        selected = state.settings.keyboardLanguage,
                        onSelect = viewModel::setLanguage,
                    )
                }
            }

            item {
                Text("Snippets & templates", style = MaterialTheme.typography.titleMedium)
            }

            if (state.snippets.isEmpty()) {
                item {
                    EmptyState(
                        title = "No snippets yet",
                        message = "Add a template and give it keywords — typing those keywords " +
                            "in the Death Code Keyboard will offer the template.",
                    )
                }
            }

            items(state.snippets, key = { it.id }) { snippet ->
                Card(
                    onClick = { editing = snippet },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(snippet.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = Languages.displayName(snippet.language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = snippet.body.lineSequence().take(3).joinToString("\n"),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Keywords: ${snippet.keywords.ifBlank { "—" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { viewModel.deleteSnippet(snippet.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        val snippet = editing
        ContentEditorDialog(
            title = if (snippet == null) "New snippet" else "Edit snippet",
            confirmLabel = "Save",
            initialMarkdown = snippet?.title.orEmpty(),
            markdownLabel = "Title",
            initialSyntax = snippet?.body.orEmpty(),
            syntaxLabel = "Template (use \${cursor} for the caret)",
            initialNotes = snippet?.keywords.orEmpty(),
            notesLabel = "Keywords (comma separated)",
            initialLanguage = snippet?.language ?: state.settings.keyboardLanguage,
            onDismiss = { creating = false; editing = null },
            showClear = snippet != null,
            onClear = { snippet?.let { viewModel.deleteSnippet(it.id) }; editing = null },
            onSave = { title, body, keywords, language, _ ->
                viewModel.saveSnippet(
                    id = snippet?.id,
                    title = title,
                    body = body.orEmpty(),
                    keywords = keywords.orEmpty(),
                    language = language ?: state.settings.keyboardLanguage,
                )
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LanguagePicker(selected: String, onSelect: (String) -> Unit) {
    Column {
        Languages.pickerOptions.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                row.forEach { (id, name) ->
                    if (id == selected) {
                        Button(onClick = { onSelect(id) }) { Text(name) }
                    } else {
                        OutlinedButton(onClick = { onSelect(id) }) { Text(name) }
                    }
                }
            }
        }
    }
}

private fun openKeyboardSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun showKeyboardPicker(context: Context) {
    val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    manager.showInputMethodPicker()
}
