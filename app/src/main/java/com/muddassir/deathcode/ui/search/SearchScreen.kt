package com.muddassir.deathcode.ui.search

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muddassir.deathcode.data.local.search.MatchField
import com.muddassir.deathcode.data.local.search.SearchResult
import com.muddassir.deathcode.domain.model.ContentSource
import com.muddassir.deathcode.ui.components.EmptyState
import com.muddassir.deathcode.ui.components.SourceBadge
import com.muddassir.deathcode.ui.rememberAppViewModel

/**
 * Global search. Everything runs against the local Room database — no network call is made
 * while searching, and the source filters only change what the local engine looks at.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    initialQuery: String = "",
) {
    val viewModel: SearchViewModel = rememberAppViewModel { SearchViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) viewModel.setQuery(initialQuery)
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search anything...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceFilterChip(
                    label = "Death Code",
                    selected = ContentSource.OFFICIAL in state.filters.sources,
                    onClick = { viewModel.toggleSource(ContentSource.OFFICIAL) },
                )
                SourceFilterChip(
                    label = "My Notes",
                    selected = ContentSource.PRIVATE in state.filters.sources,
                    onClick = { viewModel.toggleSource(ContentSource.PRIVATE) },
                )
                SourceFilterChip(
                    label = "Community",
                    selected = ContentSource.COMMUNITY in state.filters.sources,
                    onClick = { viewModel.toggleSource(ContentSource.COMMUNITY) },
                )
            }

            when {
                state.query.isBlank() -> EmptyState(
                    title = "Search your knowledge",
                    message = "Try \"decimal to binary\", a keyword like \"bfs\", or a note you wrote.",
                )

                state.results.isEmpty() && !state.searching -> EmptyState(
                    title = "No matches",
                    message = "Nothing in the selected local sources matched \"${state.query}\".",
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(state.results, key = { it.node.id }) { result ->
                        SearchResultCard(result = result, onClick = { onOpenNode(result.node.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SearchResultCard(result: SearchResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(result.node.title, style = MaterialTheme.typography.titleMedium)
                SourceBadge(result.node.source)
            }

            Text(
                text = result.path.joinToString(" > "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            result.snippet?.takeIf { it.isNotBlank() }?.let { snippet ->
                Text(
                    text = snippet.lineSequence().take(3).joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Text(
                text = "Matched in: " + result.matchedFields.joinToString(" · ") { fieldName(it) },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun fieldName(field: MatchField): String = when (field) {
    MatchField.TITLE -> "Title"
    MatchField.KEYWORD -> "Keyword"
    MatchField.SYNTAX -> "Syntax"
    MatchField.NOTE -> "Note"
    MatchField.CONTENT -> "Content"
    MatchField.CATEGORY -> "Category"
    MatchField.FUZZY -> "Similar"
}
