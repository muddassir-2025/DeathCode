package com.muddassir.deathcode.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muddassir.deathcode.data.local.db.entity.ContentNodeEntity
import com.muddassir.deathcode.di.AppContainer
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val officialRoots: List<ContentNodeEntity> = emptyList(),
    val officialVersion: Long = 0L,
    val privateRootId: String? = null,
)

/**
 * Home state. Category cards are observed from Room, so a content synchronization is
 * reflected immediately without a manual refresh.
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val privateRootId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val state: StateFlow<HomeState> = combine(
        container.contentRepository.observeRoots(ContentSource.OFFICIAL),
        container.syncRepository.observeVersion(ContentSource.OFFICIAL),
        privateRootId,
    ) { roots, version, privateRoot ->
        HomeState(officialRoots = roots, officialVersion = version, privateRootId = privateRoot)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    init {
        viewModelScope.launch {
            privateRootId.value = container.contentRepository.ensurePrivateRoot()
        }
    }
}
