package com.github.yuchuri.snapcodejetbrains.settings

import java.awt.Color

data class SnapcodeSettings(
    var backgroundColorStart: String = "#1a1a2e",
    var backgroundColorEnd: String = "#16213e",
    var padding: Int = 40,
    var showWindowChrome: Boolean = true,
    var fontSize: Float = 14f,
    var shadowEnabled: Boolean = true,
    var theme: String = "DARK"
) {
    fun backgroundStartColor(): Color = parseHex(backgroundColorStart) ?: Color(26, 26, 46)
    fun backgroundEndColor(): Color = parseHex(backgroundColorEnd) ?: Color(22, 33, 62)

    private fun parseHex(hex: String): Color? = try {
        Color.decode(hex)
    } catch (_: NumberFormatException) {
        null
    }
}
