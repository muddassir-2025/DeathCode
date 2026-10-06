package com.muddassir.deathcode.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.repository.ContentDraft
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.domain.model.ContentSource
import com.muddassir.deathcode.ui.browse.TextInputDialog
import com.muddassir.deathcode.ui.components.EmptyState
import com.muddassir.deathcode.ui.rememberAppViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminState(
    val roots: List<ContentNodeEntity> = emptyList(),
    val version: Long = 0L,
)

class AdminViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<AdminState> = combine(
        container.contentRepository.observeRoots(ContentSource.OFFICIAL),
        container.syncRepository.observeVersion(ContentSource.OFFICIAL),
    ) { roots, version -> AdminState(roots, version) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminState())

    fun createRoot(title: String) {
        viewModelScope.launch {
            container.contentRepository.createNode(
                parentId = null,
                draft = ContentDraft(title = title),
                source = ContentSource.OFFICIAL,
            )
        }
    }

    fun publish() {
        viewModelScope.launch { container.syncRepository.publishOfficialDraft() }
    }
}

/**
 * Super Admin surface for official content.
 *
 * Edits are local drafts until "Publish new version" is pressed, mirroring the
 * Draft → Preview → Publish → Synchronize flow. The production deployment additionally
 * enforces admin authorization on the backend; the client never decides access on its own.
 */
@Composable
fun AdminScreen(
    onOpenNode: (String) -> Unit,
) {
    val viewModel: AdminViewModel = rememberAppViewModel { AdminViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Official content", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Published version: ${state.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = viewModel::publish) { Text("Publish new version") }
            }
        }

        if (state.roots.isEmpty()) {
            EmptyState(
                title = "No official categories",
                message = "Create a top level category to start building official content.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.roots, key = { it.id }) { node ->
                    Card(
                        onClick = { onOpenNode(node.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(node.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Tap to edit, nest or import Markdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    FloatingActionButton(
        onClick = { showCreate = true },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = "New category")
    }
    }

    if (showCreate) {
        TextInputDialog(
            title = "New top level category",
            label = "Title",
            onDismiss = { showCreate = false },
            onConfirm = { viewModel.createRoot(it); showCreate = false },
        )
    }
}
