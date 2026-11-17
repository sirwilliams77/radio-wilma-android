// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colours of the screen, the same set of themes as in the web panel of the Radio Wilma devices. */
data class WilmaPalette(
    val name: String,
    val clock: Color,
    val accent: Color,
    val station: Color,
    val title: Color,
    val label: Color,
    val artist: Color,
    val frame: Color,
    val divider: Color,
    val eqLow: Color,
    val eqMid: Color,
    val eqHigh: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val TextPrimary = Color(0xFFF5F7FA)
val TextCity = Color(0xFFDEE2E8)
val Background = Color(0xFF000000)
val PanelDark = Color(0xFF0B141B)

val Themes: List<WilmaPalette> = listOf(
    WilmaPalette(
        name = "Niebieski",
        clock = c(0xF5F7FA), accent = c(0x16A8F0), station = c(0x16A8F0), title = c(0x28BAEE),
        label = c(0x969CA6), artist = c(0xF5F7FA), frame = c(0x1678B4), divider = c(0x3A404C),
        eqLow = c(0x2ECC40), eqMid = c(0xFACC15), eqHigh = c(0xEF4444),
    ),
    WilmaPalette(
        name = "Bursztynowy",
        clock = c(0xF5F7FA), accent = c(0xFFB300), station = c(0xFFB300), title = c(0xFFC94D),
        label = c(0xA89A84), artist = c(0xF5F7FA), frame = c(0x8A6420), divider = c(0x4A3D24),
        eqLow = c(0xFFB300), eqMid = c(0xFF8F00), eqHigh = c(0xFF5A1F),
    ),
    WilmaPalette(
        name = "Zielony retro",
        clock = c(0x7DFF6B), accent = c(0x3DDC84), station = c(0x3DDC84), title = c(0x7DFF6B),
        label = c(0x6F9A6A), artist = c(0xD8FFD2), frame = c(0x2F7A3A), divider = c(0x244A2A),
        eqLow = c(0x3DDC84), eqMid = c(0xB6F542), eqHigh = c(0xF5E642),
    ),
    WilmaPalette(
        name = "Biało-szary",
        clock = c(0xF5F7FA), accent = c(0xCFD3D9), station = c(0xFFFFFF), title = c(0xCFD3D9),
        label = c(0x8A8F98), artist = c(0xF5F7FA), frame = c(0x6A6E75), divider = c(0x3A3D42),
        eqLow = c(0xE6E8EB), eqMid = c(0xB9BDC3), eqHigh = c(0xFFFFFF),
    ),
    WilmaPalette(
        name = "Czerwony",
        clock = c(0xF5F7FA), accent = c(0xFF4D4D), station = c(0xFF4D4D), title = c(0xFF8080),
        label = c(0xA08A8A), artist = c(0xF5F7FA), frame = c(0x8A2F2F), divider = c(0x4A2626),
        eqLow = c(0xFF8080), eqMid = c(0xFF4D4D), eqHigh = c(0xFFFFFF),
    ),
)

fun paletteByName(name: String): WilmaPalette = Themes.firstOrNull { it.name == name } ?: Themes[0]

val LocalPalette = staticCompositionLocalOf { Themes[0] }
