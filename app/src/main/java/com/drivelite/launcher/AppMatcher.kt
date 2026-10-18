package com.drivelite.launcher

/** The four buttons whose target app can be auto-detected or chosen by the user. */
enum class Slot { MAPS, RADIO, PHONE, MUSIC }

data class LabeledApp(val label: String, val packageName: String)

/**
 * Heuristics for finding the right app on unknown head units. Pure Kotlin — unit-tested.
 * Car screens ship vendor apps with unpredictable package names, so we try known
 * packages first, then fall back to matching the visible app label.
 */
object AppMatcher {

    const val GOOGLE_MAPS = "com.google.android.apps.maps"

    /** Radio apps seen on common Android head units (FYT/SYU, MTCD, AOSP/Qualcomm/MediaTek). */
    val RADIO_PACKAGES = listOf(
        "com.syu.radio",
        "com.microntek.radio",
        "com.android.fmradio",
        "com.caf.fmradio",
        "com.mediatek.fmradio",
        "com.android.radio",
    )

    val PACKAGES: Map<Slot, List<String>> = mapOf(
        Slot.MAPS to listOf(GOOGLE_MAPS, "com.waze"),
        Slot.RADIO to RADIO_PACKAGES,
        Slot.PHONE to listOf("com.syu.bt", "com.microntek.bluetooth", "com.android.bluetooth.ui"),
        Slot.MUSIC to emptyList(),
    )

    /** Keywords in priority order (English + Arabic). */
    val KEYWORDS: Map<Slot, List<String>> = mapOf(
        Slot.MAPS to listOf("maps", "خرائط", "navigation", "navi", "ملاحة"),
        Slot.RADIO to listOf("radio", "راديو", "fm", "الراديو"),
        Slot.PHONE to listOf("phone", "هاتف", "dialer", "bluetooth", "بلوتوث", "اتصال"),
        Slot.MUSIC to listOf("music", "موسيقى", "موسيقي", "audio", "player", "مشغل"),
    )

    /**
     * Returns the package of the best label match, or null.
     * Short keywords (< 4 chars, e.g. "fm") must start a word, so "fm" matches
     * "FM Radio" / "FMRadio" but not "Perfmon".
     */
    fun bestByLabel(apps: List<LabeledApp>, keywords: List<String>, excludePackage: String? = null): String? {
        val candidates = apps.filter { it.packageName != excludePackage }
        for (raw in keywords) {
            val kw = raw.lowercase()
            val hit = candidates.firstOrNull { matches(it.label, kw) }
            if (hit != null) return hit.packageName
        }
        return null
    }

    internal fun matches(label: String, keyword: String): Boolean {
        val l = label.lowercase()
        if (keyword.length >= 4) return l.contains(keyword)
        return l.split(Regex("[^\\p{L}\\p{N}]+")).any { it.startsWith(keyword) }
    }
}
