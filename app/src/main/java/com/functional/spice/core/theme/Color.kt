package com.functional.spice.core.theme

import androidx.compose.ui.graphics.Color

// Design tokens v3 ("neo" look): cool grey background, white rounded cards, near-black ink for
// primary actions, lime as the accent. Light mode only.
// The names are kept from v2 so every screen picks up the new palette; read them by role:
//   WarmAlabaster = page background, SoftCream = card / surface, DeepEspresso & WarmCardamom = ink.

// Accent
val Lime = Color(0xFFC3EF3C)             // Highlight cards, active switch, badges, checkout button
val LimeSoft = Color(0xFFF0FAD2)         // Selected counter card, lime badges' background
val LimeInk = Color(0xFF3F5A00)          // Text on LimeSoft

// Base
val WarmAlabaster = Color(0xFFF2F2F5)    // Background (cool grey)
val SoftCream = Color(0xFFFFFFFF)        // Surface: white cards
val SoftCreamBorder = Color(0xFFE9E9EE)  // Hairlines, keypad and input outlines
val SelectedCard = LimeSoft              // Selected counter card (with ink border)
val DeepEspresso = Color(0xFF1B1B1D)     // Primary: dark cards, selected chips
val WarmCardamom = Color(0xFF1B1B1D)     // Actions: pill buttons, links, focus
val Charcoal = Color(0xFF151517)         // Text
val MutedStone = Color(0xFF8A8A92)       // Text secondary
val StoneBody = Color(0xFF55555C)        // Secondary body text, icons
val StoneFaint = Color(0xFFB4B4BB)       // Hints, disabled
val SwitchOff = Color(0xFFE2E2E8)
val FieldFill = Color(0xFFF4F4F7)        // Grey list rows and fields inside white cards

// On ink (hero cards, checkout bar)
val OnEspressoMuted = Color(0xFF9C9CA4)
val OnEspressoSoft = Color(0xFFE4E4EA)
val EspressoDivider = Color(0xFF2E2E33)
val EspressoOutline = Color(0xFF3A3A40)

// Status
val SuccessBg = LimeSoft
val SuccessFg = LimeInk
val SuccessDot = Color(0xFF8CCB12)
val SuccessBorder = Color(0xFFDDF2A6)
val WarningBg = Color(0xFFFFF3DD)
val WarningFg = Color(0xFF7A4E0E)
val WarningBorder = Color(0xFFF5DFB4)
val WarningOutline = Color(0xFFE0BE80)
val WarningIcon = Color(0xFFE09A1F)
val OfflineBg = Color(0xFFEDEDF1)
val OfflineFg = Color(0xFF55555C)
val Danger = Color(0xFFD92D20)

val Scrim = Color(0x73151517)            // #151517 at 45%
