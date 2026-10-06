package com.muddassir.deathcode.ui.keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muddassir.deathcode.core.KeywordList
import com.muddassir.deathcode.data.local.db.entity.SnippetEntity
import com.muddassir.deathcode.data.repository.AppSettings
import com.muddassir.deathcode.data.repository.SnippetDraft
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KeyboardUiState(
    val settings: AppSettings = AppSettings(),
    val snippets: List<SnippetEntity> = emptyList(),
)

/**
 * Keyboard preferences and snippet management.
 *
 * The IME service reads the same DataStore, so settings changed here apply to the keyboard
 * without restarting anything.
 */
class KeyboardViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<KeyboardUiState> = combine(
        container.settingsRepository.settings,
        container.snippetRepository.observeAll(),
    ) { settings, snippets ->
        KeyboardUiState(settings = settings, snippets = snippets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KeyboardUiState())

    fun setShowSuggestions(enabled: Boolean) = launch { container.settingsRepository.setShowSuggestions(enabled) }

    fun setShowSymbolRow(enabled: Boolean) = launch { container.settingsRepository.setShowSymbolRow(enabled) }

    fun setKeyHeight(dp: Int) = launch { container.settingsRepository.setKeyHeightDp(dp) }

    fun setHaptics(enabled: Boolean) = launch { container.settingsRepository.setHaptics(enabled) }

    fun setSound(enabled: Boolean) = launch { container.settingsRepository.setSound(enabled) }

    fun setLanguage(language: String) = launch { container.settingsRepository.setKeyboardLanguage(language) }

    fun saveSnippet(
        id: String?,
        title: String,
        body: String,
        keywords: String,
        language: String,
    ) {
        launch {
            container.snippetRepository.save(
                draft = SnippetDraft(
                    title = title,
                    language = language,
                    body = body,
                    keywords = KeywordList.parse(keywords),
                ),
                source = ContentSource.PRIVATE,
                id = id,
            )
        }
    }

    fun deleteSnippet(id: String) = launch { container.snippetRepository.delete(id) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
