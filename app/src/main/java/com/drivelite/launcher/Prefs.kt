package com.drivelite.launcher

import android.content.ComponentName
import android.content.Context

/** Tiny SharedPreferences wrapper: theme + per-button app overrides. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("drivelite", Context.MODE_PRIVATE)

    var theme: DriveTheme
        get() = DriveTheme.fromId(sp.getString(KEY_THEME, null))
        set(value) = sp.edit().putString(KEY_THEME, value.id).apply()

    fun shortcut(slot: Slot): ComponentName? =
        sp.getString(key(slot), null)?.let { ComponentName.unflattenFromString(it) }

    fun setShortcut(slot: Slot, component: ComponentName?) {
        val e = sp.edit()
        if (component == null) e.remove(key(slot)) else e.putString(key(slot), component.flattenToString())
        e.apply()
    }

    fun resetShortcuts() {
        val e = sp.edit()
        Slot.entries.forEach { e.remove(key(it)) }
        e.apply()
    }

    private fun key(slot: Slot) = "slot_" + slot.name.lowercase()

    private companion object {
        const val KEY_THEME = "theme"
    }
}
