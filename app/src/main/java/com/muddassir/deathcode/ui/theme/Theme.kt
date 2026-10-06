package com.muddassir.deathcode.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.muddassir.deathcode.data.repository.ThemeMode

/**
 * Colours for syntax highlighting. Exposed through a composition local so the highlighter
 * adapts to the active theme instead of hard-coding a dark palette.
 */
data class CodeColors(
    val background: Color,
    val keyword: Color,
    val type: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val function: Color,
    val annotation: Color,
    val plain: Color,
)

val LocalCodeColors = staticCompositionLocalOf {
    CodeColors(
        background = CodeBackground,
        keyword = CodeKeyword,
        type = CodeType,
        string = CodeString,
        comment = CodeComment,
        number = CodeNumber,
        function = CodeFunction,
        annotation = CodeAnnotation,
        plain = CodePlain,
    )
}

/**
 * Colours for the keyboard surface.
 *
 * The keyboard keeps its own palette instead of reusing the Material surface colours: it
 * needs a strong, glanceable separation between the base, the character keys, the
 * functional keys and the accent key, which the shared surfaces are too low-contrast for.
 */
data class KeyboardColors(
    val base: Color,
    val key: Color,
    val functionKey: Color,
    val accent: Color,
    val onAccent: Color,
    val pressed: Color,
    val suggestionBar: Color,
    val keyText: Color,
    val mutedText: Color,
)

private val DarkKeyboardColors = KeyboardColors(
    base = KeyBaseDark,
    key = KeyFaceDark,
    functionKey = KeyFunctionDark,
    accent = KeyAccentDark,
    onAccent = OnKeyAccentDark,
    pressed = KeyPressedDark,
    suggestionBar = KeyBarDark,
    keyText = KeyTextDark,
    mutedText = KeyMutedDark,
)

private val LightKeyboardColors = KeyboardColors(
    base = KeyBaseLight,
    key = KeyFaceLight,
    functionKey = KeyFunctionLight,
    accent = KeyAccentLight,
    onAccent = OnKeyAccentLight,
    pressed = KeyPressedLight,
    suggestionBar = KeyBarLight,
    keyText = KeyTextLight,
    mutedText = KeyMutedLight,
)

val LocalKeyboardColors = staticCompositionLocalOf { DarkKeyboardColors }

private val DarkScheme = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = Color(0xFF08210F),
    secondary = TerminalCyan,
    onSecondary = Color(0xFF04222A),
    tertiary = TerminalAmber,
    background = Ink,
    onBackground = InkText,
    surface = InkElevated,
    onSurface = InkText,
    surfaceVariant = InkCard,
    onSurfaceVariant = InkMuted,
    outline = InkOutline,
    outlineVariant = InkOutline,
    error = Color(0xFFE06C75),
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = Color(0xFFB7791F),
    background = LightSurface,
    onBackground = LightText,
    surface = Color.White,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = Color(0xFFC0392B),
)

private val DarkCodeColors = CodeColors(
    background = CodeBackground,
    keyword = CodeKeyword,
    type = CodeType,
    string = CodeString,
    comment = CodeComment,
    number = CodeNumber,
    function = CodeFunction,
    annotation = CodeAnnotation,
    plain = CodePlain,
)

private val LightCodeColors = CodeColors(
    background = Color(0xFFF3F5F7),
    keyword = Color(0xFF8E44AD),
    type = Color(0xFF1F6F8B),
    string = Color(0xFF2E7D5B),
    comment = Color(0xFF7A8794),
    number = Color(0xFFB7791F),
    function = Color(0xFF1F6FB2),
    annotation = Color(0xFFC0392B),
    plain = Color(0xFF2C3542),
)

/**
 * Death Code theme. Defaults to the dark, developer-tool look while still honouring the
 * user's explicit choice from Settings.
 */
@Composable
fun DeathCodeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    CompositionLocalProvider(
        LocalCodeColors provides if (darkTheme) DarkCodeColors else LightCodeColors,
        LocalKeyboardColors provides if (darkTheme) DarkKeyboardColors else LightKeyboardColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = DeathCodeTypography,
            content = content,
        )
    }
}
