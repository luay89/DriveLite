package com.drivelite.launcher

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Theme picker, Home controls (set / return to original), shortcut reset, about. */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)
        findViewById<TextView>(R.id.header_title).setText(R.string.settings_title)
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_set_home).setOnClickListener { HomeHelper.requestDefaultHome(this) }
        findViewById<View>(R.id.btn_return_home).setOnClickListener { HomeHelper.openHomeSettings(this) }
        findViewById<View>(R.id.btn_reset_shortcuts).setOnClickListener {
            prefs.resetShortcuts()
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        }

        val dm = resources.displayMetrics
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
        findViewById<TextView>(R.id.about).text =
            getString(R.string.version_fmt, version, Build.VERSION.RELEASE, dm.widthPixels, dm.heightPixels)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val t = prefs.theme
        Ui.paintWindow(this, t)
        findViewById<View>(R.id.root).setBackgroundColor(t.bg)
        findViewById<TextView>(R.id.header_title).setTextColor(t.text)
        findViewById<ImageView>(R.id.btn_back).setColorFilter(t.text)

        listOf(R.id.sec_theme, R.id.sec_home, R.id.sec_shortcuts, R.id.sec_about)
            .forEach { findViewById<TextView>(it).setTextColor(t.accent) }
        listOf(R.id.home_status, R.id.return_hint, R.id.shortcuts_hint, R.id.about)
            .forEach { findViewById<TextView>(it).setTextColor(t.textDim) }
        listOf(R.id.btn_set_home, R.id.btn_return_home, R.id.btn_reset_shortcuts)
            .forEach { Ui.styleButton(findViewById(it), t) }

        val isHome = HomeHelper.isDefaultHome(this)
        findViewById<TextView>(R.id.home_status).apply {
            setText(if (isHome) R.string.home_status_yes else R.string.home_status_no)
            setTextColor(if (isHome) t.accent else t.text)
        }
        findViewById<View>(R.id.btn_set_home).visibility = if (isHome) View.GONE else View.VISIBLE

        renderThemes(t)
        renderOtherHomes(t)
    }

    private fun renderThemes(current: DriveTheme) {
        val row = findViewById<LinearLayout>(R.id.theme_row)
        row.removeAllViews()
        val names = mapOf(
            DriveTheme.MIDNIGHT to R.string.theme_midnight,
            DriveTheme.GRAPHITE to R.string.theme_graphite,
            DriveTheme.OCEAN to R.string.theme_ocean,
            DriveTheme.AMBER to R.string.theme_amber,
        )
        val gap = Ui.dp(row, 6f).toInt()
        for (option in DriveTheme.entries) {
            val selected = option == current
            val chip = TextView(this).apply {
                setText(names.getValue(option))
                gravity = Gravity.CENTER
                textSize = 15f
                setTextColor(option.text)
                isClickable = true
                isFocusable = true
                background = Ui.pressable(
                    this, option.surface, option.accent.withAlpha(80), 14f,
                    stroke = if (selected) option.accent else option.surface,
                )
                setOnClickListener {
                    prefs.theme = option
                    render()
                }
            }
            row.addView(chip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = gap
                marginEnd = gap
            })
        }
    }

    private fun renderOtherHomes(t: DriveTheme) {
        val box = findViewById<LinearLayout>(R.id.other_homes)
        box.removeAllViews()
        val homes = HomeHelper.otherHomes(this)
        if (homes.isEmpty()) {
            box.addView(TextView(this).apply {
                setText(R.string.no_other_homes)
                setTextColor(t.textDim)
                textSize = 14f
            })
            return
        }
        val height = Ui.dp(box, 52f).toInt()
        val top = Ui.dp(box, 10f).toInt()
        for (info in homes) {
            val label = info.loadLabel(packageManager).toString()
            box.addView(TextView(this).apply {
                text = getString(R.string.open_now, label)
                gravity = Gravity.CENTER
                textSize = 16f
                isClickable = true
                isFocusable = true
                Ui.styleButton(this, t)
                setOnClickListener {
                    if (!HomeHelper.launchHome(this@SettingsActivity, info)) HomeHelper.showManualSteps(this@SettingsActivity)
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height).apply { topMargin = top })
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        render()
    }
}
