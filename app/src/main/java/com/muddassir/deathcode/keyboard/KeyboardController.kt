package com.muddassir.deathcode.keyboard

import com.muddassir.deathcode.data.repository.KeyboardSuggestion
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.domain.model.ContentSource
import com.muddassir.deathcode.keyboard.input.TextEditor
import com.muddassir.deathcode.keyboard.snippets.TemplateInserter
import com.muddassir.deathcode.syntax.Languages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shift behaviour: off, one-shot, or caps lock. */
enum class ShiftState { OFF, ON, CAPS }

data class KeyboardState(
    val suggestions: List<KeyboardSuggestion> = emptyList(),
    val typedWord: String = "",
    val language: String = "cpp",
    val showSuggestions: Boolean = true,
    val showSymbolRow: Boolean = true,
    val keyHeightDp: Int = 52,
    val haptics: Boolean = true,
    val sound: Boolean = false,
    val sources: Set<ContentSource> = setOf(
        ContentSource.OFFICIAL,
        ContentSource.PRIVATE,
        ContentSource.COMMUNITY,
    ),
    val shift: ShiftState = ShiftState.OFF,
    val symbolsPage: Boolean = false,
)

/**
 * All keyboard behaviour, independent of how the view is rendered.
 *
 * The controller owns three concerns:
 * 1. mirroring the user's keyboard settings from DataStore,
 * 2. resolving offline snippet suggestions for the word being typed,
 * 3. committing text and expanding templates through the [TextEditor].
 */
class KeyboardController(
    private val container: AppContainer,
    private val editorProvider: () -> TextEditor,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    private var suggestJob: Job? = null

    private val editor: TextEditor get() = editorProvider()

    init {
        scope.launch {
            container.settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        language = settings.keyboardLanguage,
                        showSuggestions = settings.showSuggestions,
                        showSymbolRow = settings.showSymbolRow,
                        keyHeightDp = settings.keyHeightDp,
                        haptics = settings.hapticsEnabled,
                        sound = settings.soundEnabled,
                        sources = settings.searchSources,
                    )
                }
                refreshSuggestions()
            }
        }
    }

    // ------------------------------------------------------------ text input

    fun onCharacter(char: String) {
        val text = applyShift(char)
        editor.commit(text)
        if (_state.value.shift == ShiftState.ON) {
            _state.update { it.copy(shift = ShiftState.OFF) }
        }
        refreshSuggestions()
    }

    fun onBackspace() {
        editor.deleteBackwards(1)
        refreshSuggestions()
    }

    fun onSpace() {
        editor.commit(" ")
        if (_state.value.shift == ShiftState.ON) {
            _state.update { it.copy(shift = ShiftState.OFF) }
        }
        refreshSuggestions()
    }

    fun onEnter() {
        editor.sendEnter()
        refreshSuggestions()
    }

    fun onSymbol(symbol: String) {
        editor.commit(symbol)
        refreshSuggestions()
    }

    fun onShift() {
        _state.update {
            it.copy(
                shift = when (it.shift) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.CAPS
                    ShiftState.CAPS -> ShiftState.OFF
                },
            )
        }
    }

    fun onToggleSymbolsPage() {
        _state.update { it.copy(symbolsPage = !it.symbolsPage) }
    }

    // ------------------------------------------------------------ snippets

    fun applySuggestion(suggestion: KeyboardSuggestion) {
        TemplateInserter.insert(editor, _state.value.typedWord, suggestion.template)
        scope.launch { runCatching { container.keyboardRepository.recordUsage(suggestion) } }
        refreshSuggestions()
    }

    /**
     * Inserts a template directly (used by the keyboard's own quick actions), replacing the
     * keyword being typed when there is one.
     */
    fun applyTemplate(template: String) {
        TemplateInserter.insert(editor, _state.value.typedWord, template)
        refreshSuggestions()
    }

    fun onLanguageCycle() {
        val options = Languages.all.map { it.id }
        if (options.isEmpty()) return
        val currentIndex = options.indexOf(_state.value.language)
        val next = options[(currentIndex + 1).mod(options.size)]
        _state.update { it.copy(language = next) }
        scope.launch { container.settingsRepository.setKeyboardLanguage(next) }
        refreshSuggestions()
    }

    /** Called by the service whenever the editor content/caret may have changed. */
    fun onEditorChanged() {
        refreshSuggestions()
    }

    fun dispose() {
        suggestJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    // ------------------------------------------------------------ internals

    private fun applyShift(char: String): String = when (_state.value.shift) {
        ShiftState.OFF -> char.lowercase()
        ShiftState.ON, ShiftState.CAPS -> char.uppercase()
    }

    private fun refreshSuggestions() {
        val word = TemplateInserter.trailingWord(editor.textBeforeCursor(64))
        _state.update { it.copy(typedWord = word) }

        suggestJob?.cancel()
        if (word.isEmpty()) {
            _state.update { it.copy(suggestions = emptyList()) }
            return
        }

        suggestJob = scope.launch {
            delay(SUGGEST_DEBOUNCE_MS)
            val current = _state.value
            val suggestions = runCatching {
                container.keyboardRepository.suggestions(
                    typed = word,
                    language = current.language,
                    sources = current.sources,
                )
            }.getOrDefault(emptyList())
            _state.update {
                if (it.typedWord == word) it.copy(suggestions = suggestions) else it
            }
        }
    }

    private companion object {
        const val SUGGEST_DEBOUNCE_MS = 60L
    }
}
