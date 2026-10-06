package com.muddassir.deathcode.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Dark programmer palette (the default look) --------------------------------

val TerminalGreen = Color(0xFF7BD88F)
val TerminalCyan = Color(0xFF5FD7E5)
val TerminalAmber = Color(0xFFE5C07B)
val TerminalMagenta = Color(0xFFC678DD)

val Ink = Color(0xFF0E1116)
val InkElevated = Color(0xFF151A21)
val InkCard = Color(0xFF1B222B)
val InkOutline = Color(0xFF2C3542)
val InkText = Color(0xFFE6EDF3)
val InkMuted = Color(0xFF9BA7B4)

// ---- Light palette -------------------------------------------------------------

val LightPrimary = Color(0xFF2E7D5B)
val LightSecondary = Color(0xFF1F6F8B)
val LightSurface = Color(0xFFFBFCFD)
val LightSurfaceVariant = Color(0xFFEDF1F4)
val LightText = Color(0xFF151A21)
val LightMuted = Color(0xFF55606C)
val LightOutline = Color(0xFFC7D0D8)

// ---- Code block colours (used by the highlighter) -----------------------------

val CodeBackground = Color(0xFF0B0F14)
val CodeKeyword = Color(0xFFC678DD)
val CodeType = Color(0xFF5FD7E5)
val CodeString = Color(0xFF98C379)
val CodeComment = Color(0xFF6B7683)
val CodeNumber = Color(0xFFE5C07B)
val CodeFunction = Color(0xFF61AFEF)
val CodeAnnotation = Color(0xFFE06C75)
val CodePlain = Color(0xFFD7DEE6)

// ---- Keyboard palette (dark) ---------------------------------------------------
//
// The keyboard does not reuse the Material surface colours: reusing them made the keys
// nearly the same value as the keyboard itself, so nothing read as a distinct, raised
// target. These tones are chosen for clear separation between base, key, function key and
// accent, and for comfortable contrast for the white-on-dark label text.

/** Keyboard base — darker than the keys so they read as raised. */
val KeyBaseDark = Color(0xFF0A0E13)
/** Regular character key. */
val KeyFaceDark = Color(0xFF222D39)
/** Quiet functional key (shift, backspace, ?123, language). */
val KeyFunctionDark = Color(0xFF161D26)
/** Accent key (enter) and highlights. */
val KeyAccentDark = Color(0xFF7BD88F)
val OnKeyAccentDark = Color(0xFF07200E)
/** A key while it is being pressed. */
val KeyPressedDark = Color(0xFF3C4C60)
/** Strip behind the word suggestions. */
val KeyBarDark = Color(0xFF121922)
val KeyTextDark = Color(0xFFEAF1F8)
val KeyMutedDark = Color(0xFF93A1B1)

// ---- Keyboard palette (light) --------------------------------------------------

val KeyBaseLight = Color(0xFFDCE2E9)
val KeyFaceLight = Color(0xFFFFFFFF)
val KeyFunctionLight = Color(0xFFC9D2DB)
val KeyAccentLight = Color(0xFF2E7D5B)
val OnKeyAccentLight = Color(0xFFFFFFFF)
val KeyPressedLight = Color(0xFFAEBBC9)
val KeyBarLight = Color(0xFFE7ECF1)
val KeyTextLight = Color(0xFF151A21)
val KeyMutedLight = Color(0xFF4C5866)
