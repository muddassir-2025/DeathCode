package com.muddassir.deathcode

import android.app.Application
import com.muddassir.deathcode.di.AppContainer
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Exposes the [AppContainer] to both the Compose UI and the keyboard process (the IME is a
 * separate service in the same app process group, so it shares this container).
 */
class DeathCodeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Seed official content, snippets and the private root in the background.
        container.applicationScope.launch { container.ensureInitialized() }
    }
}
