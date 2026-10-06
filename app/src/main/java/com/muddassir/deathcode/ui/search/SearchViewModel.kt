package com.muddassir.deathcode.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muddassir.deathcode.data.local.search.SearchFilters
import com.muddassir.deathcode.data.local.search.SearchResult
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: List<SearchResult> = emptyList(),
    val searching: Boolean = false,
)

/**
 * Debounced, offline search. The search source filters are persisted so the user's choice
 * survives restarts, and changing them never triggers a backend request.
 */
class SearchViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _state.update { it.copy(filters = SearchFilters(settings.searchSources)) }
                runSearch()
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        runSearch()
    }

    fun toggleSource(source: ContentSource) {
        viewModelScope.launch {
            val enabled = source !in _state.value.filters.sources
            container.settingsRepository.setSearchSource(source, enabled)
        }
    }

    private fun runSearch() {
        searchJob?.cancel()
        val query = _state.value.query
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true) }
            delay(SEARCH_DEBOUNCE_MS)
            val results = container.searchEngine.search(query, _state.value.filters)
            _state.update { it.copy(results = results, searching = false) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 180L
    }
}
