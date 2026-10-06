package com.muddassir.deathcode.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muddassir.deathcode.data.repository.AppSettings
import com.muddassir.deathcode.data.repository.AuthSession
import com.muddassir.deathcode.data.repository.SyncState
import com.muddassir.deathcode.data.repository.ThemeMode
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val session: AuthSession = AuthSession(),
    val officialVersion: Long = 0L,
    val communityVersion: Long = 0L,
    val syncing: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val syncing = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        container.settingsRepository.settings,
        container.authRepository.session,
        container.syncRepository.observeVersion(ContentSource.OFFICIAL),
        container.syncRepository.observeVersion(ContentSource.COMMUNITY),
        syncing,
    ) { settings, session, official, community, isSyncing ->
        SettingsUiState(
            settings = settings,
            session = session,
            officialVersion = official,
            communityVersion = community,
            syncing = isSyncing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = launch { container.settingsRepository.setThemeMode(mode) }

    fun setSearchSource(source: ContentSource, enabled: Boolean) =
        launch { container.settingsRepository.setSearchSource(source, enabled) }

    fun setAutoCheckUpdates(enabled: Boolean) =
        launch { container.settingsRepository.setAutoCheckUpdates(enabled) }

    fun saveBackendUrl(url: String) = launch {
        container.settingsRepository.setBackendBaseUrl(url)
        container.settingsRepository.setLastSyncMessage("Backend URL saved")
    }

    fun signOut() = launch { container.authRepository.signOut() }

    fun checkForUpdates() {
        viewModelScope.launch {
            syncing.value = true
            val result = container.syncRepository.checkAndSync(ContentSource.OFFICIAL)
            container.settingsRepository.setLastSyncMessage(describe(result))
            syncing.value = false
        }
    }

    private fun describe(state: SyncState): String = when (state) {
        is SyncState.UpToDate -> "Up to date (version ${state.version})"
        is SyncState.UpdateAvailable -> "Update available (version ${state.version})"
        is SyncState.Syncing -> "Updating..."
        is SyncState.Failed -> "Sync failed — will retry later (${state.message})"
        is SyncState.NotConfigured -> state.message
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
