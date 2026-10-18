package com.drivelite.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens the app behind each button. Order of attempts:
 *  1. the app the user assigned (long-press),
 *  2. well-known packages / standard Android intents,
 *  3. label match among installed apps,
 *  4. a clear dialog offering to pick an app — never a silent failure.
 */
class AppLauncher(private val activity: Activity, private val prefs: Prefs) {

    fun open(slot: Slot) {
        prefs.shortcut(slot)?.let { if (startComponent(it)) return }

        val opened = AppMatcher.PACKAGES[slot].orEmpty().any { startPackage(it) } || when (slot) {
            Slot.MAPS -> startIntent(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")))
            Slot.PHONE -> startIntent(Intent(Intent.ACTION_DIAL))
            Slot.MUSIC -> startIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
            Slot.RADIO -> false
        }
        if (opened) return

        // Label search runs off the UI thread.
        AppsRepository.loadAsync(activity) { apps ->
            if (activity.isFinishing || activity.isDestroyed) return@loadAsync
            val labeled = apps.map { LabeledApp(it.label, it.component.packageName) }
            val pkg = AppMatcher.bestByLabel(labeled, AppMatcher.KEYWORDS[slot].orEmpty(), activity.packageName)
            val entry = pkg?.let { p -> apps.firstOrNull { it.component.packageName == p } }
            if (entry == null || !startComponent(entry.component)) showNotFound(slot)
        }
    }

    fun openApp(entry: AppEntry) {
        if (!startComponent(entry.component)) toast(R.string.cannot_open)
    }

    /** Long-press: let the user bind any installed app to a button. */
    fun pick(slot: Slot) {
        AppsRepository.loadAsync(activity) { apps ->
            if (activity.isFinishing || activity.isDestroyed) return@loadAsync
            val labels: Array<CharSequence> = apps.map { it.label as CharSequence }.toTypedArray()
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.pick_title, activity.getString(labelOf(slot))))
                .setItems(labels) { _, which ->
                    prefs.setShortcut(slot, apps[which].component)
                    toast(R.string.saved)
                }
                .setNeutralButton(R.string.pick_auto) { _, _ ->
                    prefs.setShortcut(slot, null)
                    toast(R.string.saved)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun showNotFound(slot: Slot) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.not_found_title, activity.getString(labelOf(slot))))
            .setMessage(R.string.not_found_msg)
            .setPositiveButton(R.string.choose_app) { _, _ -> pick(slot) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startPackage(pkg: String): Boolean {
        val intent = activity.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return startIntent(intent)
    }

    private fun startComponent(cn: ComponentName): Boolean =
        startIntent(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(cn)
                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )

    private fun startIntent(intent: Intent): Boolean = try {
        activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private fun toast(res: Int) = Toast.makeText(activity, res, Toast.LENGTH_SHORT).show()

    companion object {
        fun labelOf(slot: Slot): Int = when (slot) {
            Slot.MAPS -> R.string.tile_maps
            Slot.RADIO -> R.string.tile_radio
            Slot.PHONE -> R.string.tile_phone
            Slot.MUSIC -> R.string.tile_music
        }
    }
}
