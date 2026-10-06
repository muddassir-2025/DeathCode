package com.muddassir.deathcode.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muddassir.deathcode.BuildConfig
import com.muddassir.deathcode.data.repository.ThemeMode
import com.muddassir.deathcode.domain.model.ContentSource
import com.muddassir.deathcode.ui.rememberAppViewModel

/**
 * Application settings.
 *
 * Also surfaces synchronization state honestly: the user can always see whether content is
 * local and whether an update is currently needed.
 */
@Composable
fun SettingsScreen(onOpenAdmin: () -> Unit) {
    val viewModel: SettingsViewModel = rememberAppViewModel { SettingsViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var backendUrl by remember(state.settings.backendBaseUrl) {
        mutableStateOf(state.settings.backendBaseUrl)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Section("Appearance") {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        if (mode == state.settings.themeMode) {
                            Button(onClick = { viewModel.setTheme(mode) }) { Text(label(mode)) }
                        } else {
                            OutlinedButton(onClick = { viewModel.setTheme(mode) }) { Text(label(mode)) }
                        }
                    }
                }
            }
        }

        item {
            Section("Search sources") {
                ToggleRow(
                    label = "Death Code (official)",
                    checked = state.settings.searchOfficial,
                    onChange = { viewModel.setSearchSource(ContentSource.OFFICIAL, it) },
                )
                ToggleRow(
                    label = "My Notes (private)",
                    checked = state.settings.searchPrivate,
                    onChange = { viewModel.setSearchSource(ContentSource.PRIVATE, it) },
                )
                ToggleRow(
                    label = "Community (approved)",
                    checked = state.settings.searchCommunity,
                    onChange = { viewModel.setSearchSource(ContentSource.COMMUNITY, it) },
                )
                Text(
                    text = "These filters only affect local search. No request is sent to the backend.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section("Content synchronization") {
                Text(
                    text = "Official content version: ${state.officialVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Community content version: ${state.communityVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = state.settings.lastSyncMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("Backend API URL (Render)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.saveBackendUrl(backendUrl) }) { Text("Save URL") }
                    OutlinedButton(
                        onClick = viewModel::checkForUpdates,
                        enabled = !state.syncing,
                    ) {
                        Text(if (state.syncing) "Checking..." else "Check for updates")
                    }
                }
                ToggleRow(
                    label = "Check for updates automatically",
                    checked = state.settings.autoCheckUpdates,
                    onChange = viewModel::setAutoCheckUpdates,
                )
                Text(
                    text = "Syncing is optional. Browsing, searching, notes and the keyboard " +
                        "work with no network at all.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section("Community account") {
                Text(
                    text = if (state.session.isSignedIn) {
                        "Signed in as ${state.session.email}"
                    } else {
                        "Not signed in. An account is only needed to submit content to Community."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.session.isSignedIn) {
                    OutlinedButton(onClick = viewModel::signOut) { Text("Sign out") }
                }
            }
        }

        item {
            Section("Privacy") {
                Text(
                    text = "Your notes, private categories, snippets, templates and keyword " +
                        "mappings stay on this device. Nothing is uploaded unless you explicitly " +
                        "submit it to Community.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Section("Administration") {
                Text(
                    text = "Manage official content and publish a new content version.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onOpenAdmin) { Text("Open admin") }
            }
        }

        item {
            Section("About") {
                Text("Death Code ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Offline-first programming knowledge system and programming keyboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
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

private fun label(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.DARK -> "Dark"
    ThemeMode.LIGHT -> "Light"
}
