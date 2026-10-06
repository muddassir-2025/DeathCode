package com.muddassir.deathcode.keyboard.ui

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muddassir.deathcode.data.repository.KeyboardSuggestion
import com.muddassir.deathcode.keyboard.KeyboardController
import com.muddassir.deathcode.keyboard.KeyboardState
import com.muddassir.deathcode.keyboard.ShiftState
import com.muddassir.deathcode.syntax.Languages
import com.muddassir.deathcode.ui.theme.LocalKeyboardColors
import kotlinx.coroutines.delay

/** Whether the current key press should buzz and/or click. */
private data class KeyFeedback(val haptics: Boolean, val sound: Boolean)

private val LocalKeyFeedback = compositionLocalOf { KeyFeedback(haptics = true, sound = false) }

/**
 * How prominent a key is. Character keys carry the most weight, functional keys
 * (shift, backspace, `?123`, language) are quieter, and the enter key is the single accent.
 */
private enum class KeyTone { CHARACTER, FUNCTION, ACCENT }

private val KeyCorner = RoundedCornerShape(8.dp)

/**
 * The Death Code Keyboard surface.
 *
 * Sections are independently hideable (suggestions, symbol row) so the user can reclaim
 * vertical space, and the full QWERTY alphabet is always present.
 */
@Composable
fun DeathCodeKeyboard(
    controller: KeyboardController,
    state: KeyboardState,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalKeyboardColors.current
    val keyHeight = state.keyHeightDp.dp

    CompositionLocalProvider(LocalKeyFeedback provides KeyFeedback(state.haptics, state.sound)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.base)
                // An input method window is drawn edge to edge, *underneath* the navigation bar,
                // so the system's Back/Home/Recents buttons would otherwise sit on top of the
                // bottom row. Pad the surface by the navigation-bar inset so every key stays
                // reachable (and the strip behind the buttons still matches the keyboard).
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            if (state.showSuggestions) {
                SuggestionRow(
                    suggestions = state.suggestions,
                    onSuggestion = controller::applySuggestion,
                    onOpenSettings = onOpenSettings,
                )
            }

            if (state.showSymbolRow && !state.symbolsPage) {
                SymbolRow(onSymbol = controller::onSymbol)
            }

            if (state.symbolsPage) {
                SymbolPage(controller = controller, keyHeight = keyHeight)
            } else {
                LetterPage(controller = controller, state = state, keyHeight = keyHeight)
            }

            BottomRow(
                controller = controller,
                state = state,
                keyHeight = keyHeight,
            )

            // Breathing room above the system gesture bar.
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestions: List<KeyboardSuggestion>,
    onSuggestion: (KeyboardSuggestion) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalKeyboardColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.suggestionBar)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KeyChip(label = "⚙", onClick = onOpenSettings, wide = false)

        if (suggestions.isEmpty()) {
            Text(
                text = "Type a keyword (e.g. for, bfs, triangle)",
                style = MaterialTheme.typography.labelSmall,
                color = colors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            suggestions.forEach { suggestion ->
                SuggestionChip(suggestion = suggestion, onClick = { onSuggestion(suggestion) })
            }
        }
    }
}

@Composable
private fun SuggestionChip(suggestion: KeyboardSuggestion, onClick: () -> Unit) {
    val colors = LocalKeyboardColors.current

    Column(
        modifier = Modifier
            .clip(KeyCorner)
            .background(colors.key)
            .pressable(onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = suggestion.title,
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = suggestion.preview,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = colors.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SymbolRow(onSymbol: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KeyboardLayout.symbolRow.forEach { key ->
            SymbolKeyButton(key = key, onSymbol = onSymbol)
        }
    }
}

@Composable
private fun SymbolKeyButton(key: SymbolKey, onSymbol: (String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        KeySurface(
            modifier = Modifier
                .width(46.dp)
                .height(40.dp)
                .padding(horizontal = 2.dp),
            onClick = { onSymbol(key.primary) },
            onLongPress = if (key.alternatives.size > 1) ({ menuOpen = true }) else null,
        ) {
            Text(
                text = key.primary,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            key.alternatives.forEach { alternative ->
                DropdownMenuItem(
                    text = { Text(alternative) },
                    onClick = { menuOpen = false; onSymbol(alternative) },
                )
            }
        }
    }
}

@Composable
private fun LetterPage(
    controller: KeyboardController,
    state: KeyboardState,
    keyHeight: androidx.compose.ui.unit.Dp,
) {
    val uppercase = state.shift != ShiftState.OFF
    val shiftActive = state.shift != ShiftState.OFF

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KeyboardLayout.letterRows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (index == 2) {
                    KeySurface(
                        modifier = Modifier
                            .width(52.dp)
                            .height(keyHeight)
                            .padding(2.dp),
                        tone = if (shiftActive) KeyTone.ACCENT else KeyTone.FUNCTION,
                        onClick = controller::onShift,
                    ) {
                        Text(
                            text = if (state.shift == ShiftState.CAPS) "⇪" else "⇧",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                row.forEach { letter ->
                    KeySurface(
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .padding(2.dp),
                        onClick = { controller.onCharacter(letter.toString()) },
                    ) {
                        Text(
                            text = (if (uppercase) letter.uppercaseChar() else letter).toString(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                if (index == 2) {
                    KeySurface(
                        modifier = Modifier
                            .width(52.dp)
                            .height(keyHeight)
                            .padding(2.dp),
                        tone = KeyTone.FUNCTION,
                        onClick = controller::onBackspace,
                        repeat = true,
                    ) {
                        Text("⌫", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SymbolPage(controller: KeyboardController, keyHeight: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KeyboardLayout.symbolPageRows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Captured explicitly: inside the nested `forEach`/`Box` lambdas there is more
                // than one scope in play, so `Modifier.weight` needs an explicit receiver.
                val rowScope = this
                if (index == 2) {
                    KeySurface(
                        modifier = Modifier
                            .width(52.dp)
                            .height(keyHeight),
                        tone = KeyTone.FUNCTION,
                        onClick = controller::onBackspace,
                        repeat = true,
                    ) {
                        Text("⌫", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                row.forEach { key ->
                    var menuOpen by remember { mutableStateOf(false) }
                    Box(modifier = with(rowScope) { Modifier.weight(1f) }) {
                        KeySurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(keyHeight),
                            onClick = { controller.onSymbol(key.primary) },
                            onLongPress = if (key.alternatives.size > 1) {
                                { menuOpen = true }
                            } else {
                                null
                            },
                        ) {
                            Text(
                                text = key.primary,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            key.alternatives.forEach { alternative ->
                                DropdownMenuItem(
                                    text = { Text(alternative) },
                                    onClick = {
                                        menuOpen = false
                                        controller.onSymbol(alternative)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomRow(
    controller: KeyboardController,
    state: KeyboardState,
    keyHeight: androidx.compose.ui.unit.Dp,
) {
    val colors = LocalKeyboardColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KeySurface(
            modifier = Modifier
                .width(58.dp)
                .height(keyHeight),
            tone = if (state.symbolsPage) KeyTone.ACCENT else KeyTone.FUNCTION,
            onClick = controller::onToggleSymbolsPage,
        ) {
            Text(
                text = if (state.symbolsPage) "ABC" else "?123",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        KeySurface(
            modifier = Modifier
                .width(58.dp)
                .height(keyHeight),
            tone = KeyTone.FUNCTION,
            onClick = controller::onLanguageCycle,
        ) {
            Text(
                text = languageBadge(state.language),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = colors.accent,
                fontWeight = FontWeight.Medium,
            )
        }

        KeySurface(
            modifier = Modifier
                .weight(1f)
                .height(keyHeight),
            tone = KeyTone.ACCENT,
            onClick = controller::onEnter,
        ) {
            Text("⏎", style = MaterialTheme.typography.bodyLarge)
        }

        KeySurface(
            modifier = Modifier
                .weight(3f)
                .height(keyHeight),
            onClick = controller::onSpace,
        ) {
            Text("space", style = MaterialTheme.typography.labelSmall)
        }

        KeySurface(
            modifier = Modifier
                .weight(1f)
                .height(keyHeight),
            onClick = { controller.onSymbol(".") },
        ) {
            Text(
                ".",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
        }

        KeySurface(
            modifier = Modifier
                .weight(1f)
                .height(keyHeight),
            onClick = { controller.onSymbol(",") },
        ) {
            Text(
                ",",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private fun languageBadge(language: String): String =
    Languages.byId(language)?.displayName
        ?.take(6)
        ?: language.uppercase()

/**
 * A keyboard key with immediate press feedback, optional long press and repeat.
 *
 * The label colour comes from [LocalContentColor], set here from the key's [tone], so callers
 * only supply the label text.
 */
@Composable
private fun KeySurface(
    modifier: Modifier = Modifier,
    tone: KeyTone = KeyTone.CHARACTER,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    repeat: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = LocalKeyboardColors.current
    val feedback = LocalKeyFeedback.current
    val view = LocalView.current

    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed && repeat) {
            delay(400)
            while (pressed) {
                onClick()
                delay(55)
            }
        }
    }

    val fill = when {
        pressed -> colors.pressed
        tone == KeyTone.ACCENT -> colors.accent
        tone == KeyTone.FUNCTION -> colors.functionKey
        else -> colors.key
    }

    val labelColor = when {
        pressed -> colors.keyText
        tone == KeyTone.ACCENT -> colors.onAccent
        tone == KeyTone.FUNCTION -> colors.mutedText
        else -> colors.keyText
    }

    Box(
        modifier = modifier
            .clip(KeyCorner)
            .background(fill)
            .pointerInput(repeat, onLongPress) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        // Buzz/click on press so the key feels physical.
                        if (feedback.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (feedback.sound) view.playSoundEffect(SoundEffectConstants.CLICK)
                        // Fire immediately for every key. Autorepeat (when enabled) continues from
                        // `LaunchedEffect` above after the initial delay — previously repeat keys
                        // skipped this call, so a quick tap on backspace did nothing at all.
                        onClick()
                        tryAwaitRelease()
                        pressed = false
                    },
                    onLongPress = onLongPress?.let { callback ->
                        {
                            if (feedback.haptics) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            callback()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides labelColor) {
            content()
        }
    }
}

@Composable
private fun KeyChip(label: String, onClick: () -> Unit, wide: Boolean) {
    val colors = LocalKeyboardColors.current

    Box(
        modifier = Modifier
            .width(if (wide) 120.dp else 40.dp)
            .height(36.dp)
            .clip(KeyCorner)
            .background(colors.key)
            .pressable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.keyText)
    }
}

@Composable
private fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val feedback = LocalKeyFeedback.current
    val view = LocalView.current
    return this.pointerInput(onClick, feedback) {
        detectTapGestures(onPress = {
            if (feedback.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (feedback.sound) view.playSoundEffect(SoundEffectConstants.CLICK)
            onClick()
            tryAwaitRelease()
        })
    }
}
