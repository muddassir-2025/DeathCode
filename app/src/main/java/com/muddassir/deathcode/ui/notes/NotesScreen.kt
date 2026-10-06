package com.muddassir.deathcode.ui.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.ui.browse.BrowseScreen
import com.muddassir.deathcode.ui.rememberAppViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Resolves the user's private root once, then hands off to the recursive browser. */
class NotesViewModel(private val container: AppContainer) : ViewModel() {
    private val _rootId = MutableStateFlow<String?>(null)
    val rootId: StateFlow<String?> = _rootId.asStateFlow()

    init {
        viewModelScope.launch {
            _rootId.value = container.contentRepository.ensurePrivateRoot()
        }
    }
}

/**
 * "My Notes" is the user's completely private tree.
 *
 * It is rendered by the same recursive browser as official content, which means manual
 * creation and markdown import are available at every depth here too — the only difference
 * is that nothing in this tree is ever uploaded.
 */
@Composable
fun NotesScreen(
    onOpenNode: (String) -> Unit,
) {
    val viewModel: NotesViewModel = rememberAppViewModel { NotesViewModel(it) }
    val rootId by viewModel.rootId.collectAsStateWithLifecycle()

    val id = rootId
    if (id == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        BrowseScreen(
            nodeId = id,
            onBack = {},
            onOpenNode = onOpenNode,
            showBack = false,
        )
    }
}
