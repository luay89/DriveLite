package com.drivelite.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveThemeTest {
    @Test fun unknownIdFallsBackToDefault() {
        assertEquals(DriveTheme.DEFAULT, DriveTheme.fromId(null))
        assertEquals(DriveTheme.DEFAULT, DriveTheme.fromId("nope"))
    }

    @Test fun idsRoundTripAndAreUnique() {
        DriveTheme.entries.forEach { assertEquals(it, DriveTheme.fromId(it.id)) }
        assertEquals(4, DriveTheme.entries.map { it.id }.toSet().size)
    }

    @Test fun themesAreOpaque() {
        DriveTheme.entries.forEach { assertEquals(0xFF, (it.bg ushr 24) and 0xFF) }
    }

    @Test fun withAlphaKeepsColour() {
        val c = 0xFF5B8CFF.toInt().withAlpha(0x40)
        assertEquals(0x40, (c ushr 24) and 0xFF)
        assertEquals(0x5B8CFF, c and 0x00FFFFFF)
    }
}

class AppMatcherTest {
    private val apps = listOf(
        LabeledApp("Perfmon", "com.x.perf"),
        LabeledApp("DriveLite", "com.drivelite.launcher"),
        LabeledApp("FM Radio", "com.vendor.fm"),
        LabeledApp("الموسيقى", "com.vendor.music"),
        LabeledApp("Bluetooth", "com.vendor.bt"),
    )

    @Test fun findsRadioByShortKeywordAtWordStart() {
        assertEquals("com.vendor.fm", AppMatcher.bestByLabel(apps, listOf("fm")))
        assertFalse(AppMatcher.matches("Perfmon", "fm"))
        assertTrue(AppMatcher.matches("FMRadio", "fm"))
    }

    @Test fun findsArabicLabel() {
        assertEquals("com.vendor.music", AppMatcher.bestByLabel(apps, AppMatcher.KEYWORDS.getValue(Slot.MUSIC)))
    }

    @Test fun phoneFallsBackToBluetooth() {
        assertEquals("com.vendor.bt", AppMatcher.bestByLabel(apps, AppMatcher.KEYWORDS.getValue(Slot.PHONE)))
    }

    @Test fun excludesSelfAndReturnsNullWhenNoMatch() {
        assertNull(AppMatcher.bestByLabel(apps, listOf("drivelite"), excludePackage = "com.drivelite.launcher"))
        assertNull(AppMatcher.bestByLabel(apps, listOf("camera")))
    }
}
