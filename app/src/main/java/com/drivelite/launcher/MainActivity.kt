package com.drivelite.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextClock
import android.widget.TextView

/**
 * The Home screen. Kept deliberately light:
 *  - clock/date are framework TextClocks (system-driven, update once a minute, no timers of ours),
 *  - no background service, no animations, no wallpaper, no libraries,
 *  - work that touches PackageManager runs on a background thread.
 */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var launcher: AppLauncher
    private lateinit var tiles: List<View>
    private var appliedTheme: DriveTheme? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        launcher = AppLauncher(this, prefs)

        tiles = listOf(
            tile(R.id.tile_maps, R.drawable.ic_maps, R.string.tile_maps, Slot.MAPS),
            tile(R.id.tile_radio, R.drawable.ic_radio, R.string.tile_radio, Slot.RADIO),
            tile(R.id.tile_phone, R.drawable.ic_phone, R.string.tile_phone, Slot.PHONE),
            tile(R.id.tile_music, R.drawable.ic_music, R.string.tile_music, Slot.MUSIC),
            tile(R.id.tile_apps, R.drawable.ic_apps, R.string.tile_apps) {
                startActivity(Intent(this, AppsActivity::class.java))
            },
            tile(R.id.tile_settings, R.drawable.ic_settings, R.string.tile_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
            },
        )

        findViewById<View>(R.id.home_banner).setOnClickListener { HomeHelper.requestDefaultHome(this) }
    }

    override fun onResume() {
        super.onResume()
        val t = prefs.theme
        if (t != appliedTheme) applyTheme(t)
        findViewById<View>(R.id.home_banner).visibility =
            if (HomeHelper.isDefaultHome(this)) View.GONE else View.VISIBLE
    }

    private fun tile(id: Int, icon: Int, label: Int, slot: Slot): View =
        tile(id, icon, label) { launcher.open(slot) }.also { v ->
            v.setOnLongClickListener { launcher.pick(slot); true }
        }

    private fun tile(id: Int, icon: Int, label: Int, action: () -> Unit): View {
        val v = findViewById<View>(id)
        v.findViewById<ImageView>(R.id.tile_icon).setImageResource(icon)
        v.findViewById<TextView>(R.id.tile_label).setText(label)
        v.contentDescription = getString(label)
        v.setOnClickListener { action() }
        return v
    }

    private fun applyTheme(t: DriveTheme) {
        appliedTheme = t
        Ui.paintWindow(this, t)
        findViewById<View>(R.id.root).setBackgroundColor(t.bg)
        for (v in tiles) {
            v.background = Ui.pressable(v, t.surface, t.accent.withAlpha(70))
            v.findViewById<ImageView>(R.id.tile_icon).setColorFilter(t.accent)
            v.findViewById<TextView>(R.id.tile_label).setTextColor(t.text)
        }
        findViewById<TextClock>(R.id.clock).setTextColor(t.text)
        findViewById<TextClock>(R.id.date).setTextColor(t.textDim)
        findViewById<TextView>(R.id.home_banner).apply {
            background = Ui.pressable(this, t.accent.withAlpha(40), t.accent.withAlpha(90), 24f, t.accent.withAlpha(140))
            setTextColor(t.text)
        }
    }

    @Deprecated("Launcher: Back on the home screen does nothing.")
    @Suppress("DEPRECATION", "MissingSuperCall")
    override fun onBackPressed() {
        // Intentionally empty: the home screen must not close itself.
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // onResume refreshes the "set as home" banner.
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        AppsRepository.trimMemory()
    }
}
