package com.muddassir.deathcode.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muddassir.deathcode.data.local.db.entity.CommunitySubmissionEntity
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.data.remote.ApiException
import com.muddassir.deathcode.data.repository.AuthSession
import com.muddassir.deathcode.data.repository.CommunityDraft
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityState(
    val roots: List<ContentNodeEntity> = emptyList(),
    val submissions: List<CommunitySubmissionEntity> = emptyList(),
    val session: AuthSession = AuthSession(),
    val syncing: Boolean = false,
    val message: String? = null,
)

class CommunityViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(CommunityState())
    val state: StateFlow<CommunityState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.contentRepository.observeRoots(ContentSource.COMMUNITY).collect { roots ->
                _state.update { it.copy(roots = roots) }
            }
        }
        viewModelScope.launch {
            container.communityRepository.observeSubmissions().collect { submissions ->
                _state.update { it.copy(submissions = submissions) }
            }
        }
        viewModelScope.launch {
            container.authRepository.session.collect { session ->
                _state.update { it.copy(session = session) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun createDraft(
        title: String,
        markdown: String,
        syntax: String?,
        language: String?,
        keywords: List<String>,
    ) {
        viewModelScope.launch {
            container.communityRepository.saveDraft(
                CommunityDraft(
                    title = title,
                    markdown = markdown,
                    syntax = syntax,
                    language = language,
                    keywords = keywords,
                ),
            )
            _state.update { it.copy(message = "Draft saved locally") }
        }
    }

    fun submit(id: String) {
        viewModelScope.launch {
            val result = container.communityRepository.submit(id)
            _state.update { it.copy(message = result.message) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { container.communityRepository.delete(id) }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                val baseUrl = container.settingsRepository.settings.first().backendBaseUrl
                container.authRepository.login(baseUrl, email, password)
                _state.update { it.copy(message = "Signed in") }
            } catch (e: ApiException) {
                _state.update { it.copy(message = e.message ?: "Sign in failed") }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Sign in failed") }
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            try {
                val baseUrl = container.settingsRepository.settings.first().backendBaseUrl
                container.authRepository.signup(baseUrl, email, password, null)
                _state.update { it.copy(message = "Account created") }
            } catch (e: ApiException) {
                _state.update { it.copy(message = e.message ?: "Sign up failed") }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Sign up failed") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            container.authRepository.signOut()
            _state.update { it.copy(message = "Signed out") }
        }
    }

    /** Downloads newly approved community content. Failures never disturb local content. */
    fun syncCommunity() {
        viewModelScope.launch {
            _state.update { it.copy(syncing = true) }
            val result = container.syncRepository.checkAndSync(ContentSource.COMMUNITY)
            _state.update {
                it.copy(
                    syncing = false,
                    message = when (result) {
                        is com.muddassir.deathcode.data.repository.SyncState.UpToDate ->
                            "Up to date (version ${result.version})"
                        is com.muddassir.deathcode.data.repository.SyncState.Failed -> result.message
                        is com.muddassir.deathcode.data.repository.SyncState.NotConfigured ->
                            result.message
                        else -> null
                    },
                )
            }
        }
    }
}
