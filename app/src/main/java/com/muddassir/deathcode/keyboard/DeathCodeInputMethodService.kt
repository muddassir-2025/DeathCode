package com.muddassir.deathcode.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.muddassir.deathcode.DeathCodeApp
import com.muddassir.deathcode.MainActivity
import com.muddassir.deathcode.data.repository.ThemeMode
import com.muddassir.deathcode.keyboard.input.AndroidTextEditor
import com.muddassir.deathcode.keyboard.ui.DeathCodeKeyboard
import com.muddassir.deathcode.ui.theme.DeathCodeTheme
import kotlinx.coroutines.launch

/**
 * The Death Code Keyboard.
 *
 * Built on the standard [InputMethodService] + `InputConnection` APIs so it works with any
 * Android editor. Suggestions and templates come from the local Room database through
 * [KeyboardController] — the keyboard never needs a network request.
 *
 * The service doubles as a `LifecycleOwner` / `ViewModelStoreOwner` /
 * `SavedStateRegistryOwner` so the keyboard UI can be written in Compose.
 */
class DeathCodeInputMethodService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val editor = AndroidTextEditor { currentInputConnection }

    private lateinit var controller: KeyboardController

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        val container = (application as DeathCodeApp).container
        controller = KeyboardController(container, editorProvider = { editor })

        // The keyboard may be the first component to run, so make sure the database is
        // seeded before any suggestion is requested.
        container.applicationScope.launch { container.ensureInitialized() }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onCreateInputView(): View {
        val container = (application as DeathCodeApp).container
        val themeMode = mutableStateOf(ThemeMode.DARK)

        container.applicationScope.launch {
            container.settingsRepository.settings.collect { themeMode.value = it.themeMode }
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DeathCodeInputMethodService)
            setViewTreeViewModelStoreOwner(this@DeathCodeInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@DeathCodeInputMethodService)
            setContent {
                val theme by themeMode
                DeathCodeTheme(themeMode = theme) {
                    val state by controller.state.collectAsStateWithLifecycle()
                    DeathCodeKeyboard(
                        controller = controller,
                        state = state,
                        onOpenSettings = ::openAppSettings,
                    )
                }
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        controller.onEditorChanged()
        return composeView
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        controller.onEditorChanged()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        controller.dispose()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    /** Lets the hardware/back gesture collapse the keyboard rather than consuming it. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            requestHideSelf(0)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun openAppSettings() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }
}
