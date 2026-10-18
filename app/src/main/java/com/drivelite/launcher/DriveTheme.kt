package com.drivelite.launcher

/** The four built-in palettes. Pure Kotlin (no Android types) so it is unit-testable. */
enum class DriveTheme(
    val id: String,
    val bg: Int,
    val surface: Int,
    val accent: Int,
    val text: Int,
    val textDim: Int,
) {
    MIDNIGHT("midnight", 0xFF07090F.toInt(), 0xFF141A26.toInt(), 0xFF5B8CFF.toInt(), 0xFFF2F4F8.toInt(), 0xFF8A93A6.toInt()),
    GRAPHITE("graphite", 0xFF18191C.toInt(), 0xFF2A2C31.toInt(), 0xFFD0D4DB.toInt(), 0xFFF0F0F0.toInt(), 0xFF9A9DA3.toInt()),
    OCEAN("ocean", 0xFF04161F.toInt(), 0xFF0B2A38.toInt(), 0xFF2EC4D6.toInt(), 0xFFE6F7FA.toInt(), 0xFF7FA6B3.toInt()),
    AMBER("amber", 0xFF120C05.toInt(), 0xFF2A1D0C.toInt(), 0xFFFFB238.toInt(), 0xFFFFF3E0.toInt(), 0xFFB39874.toInt());

    companion object {
        val DEFAULT = MIDNIGHT
        fun fromId(id: String?): DriveTheme = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** Same colour with a new alpha (0..255). */
fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
