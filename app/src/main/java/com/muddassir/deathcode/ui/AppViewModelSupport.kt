package com.muddassir.deathcode.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muddassir.deathcode.di.AppContainer

/** Makes the dependency container available to every composable. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the UI in a CompositionLocalProvider.")
}

/**
 * Creates and remembers a [ViewModel] wired to the [AppContainer].
 *
 * [key] scopes the instance to the current navigation destination, which is what lets a
 * single reusable browser screen serve any node id without leaking state between them.
 */
@Composable
inline fun <reified T : ViewModel> rememberAppViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> T,
): T {
    val container = LocalAppContainer.current
    val factory = remember(container) {
        viewModelFactory {
            initializer { create(container) }
        }
    }
    return viewModel(key = key, factory = factory)
}
