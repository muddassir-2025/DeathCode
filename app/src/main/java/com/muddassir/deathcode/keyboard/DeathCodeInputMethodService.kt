package com.muddassir.deathcode.keyboard

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

        return KeyboardInputRoot(this).apply {
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
    }

    /**
     * Root view of the keyboard window.
     *
     * An input method is a [android.app.Dialog]-backed window, not an Activity, so its window
     * has no `android.R.id.content` parent. Compose therefore resolves the [LifecycleOwner] for
     * its window recomposer from the *top-most* view of the window hierarchy (Compose UI's
     * `View.contentChild`), and looks it up by walking **up** from there. Owners registered only
     * on the `ComposeView` are therefore invisible to that lookup, and composition throws
     * "ViewTreeLifecycleOwner not found from …".
     *
     * [onAttachedToWindow] runs before the Compose content attaches, so it climbs to the window
     * root and registers all three owners on both the root and this view.
     */
    private inner class KeyboardInputRoot(context: Context) : FrameLayout(context) {
        override fun onAttachedToWindow() {
            val owners = this@DeathCodeInputMethodService
            var root: View = this
            while (root.parent is View) root = root.parent as View
            for (view in arrayOf<View>(this, root)) {
                view.setViewTreeLifecycleOwner(owners)
                view.setViewTreeViewModelStoreOwner(owners)
                view.setViewTreeSavedStateRegistryOwner(owners)
            }
            super.onAttachedToWindow()
        }
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
