package com.drivelite.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings

/**
 * Everything about being (or stopping being) the Home app — only public, supported APIs.
 * The app never forces itself as default; the user always confirms in system UI.
 */
object HomeHelper {

    const val REQUEST_HOME_ROLE = 41

    private fun homeIntent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    fun isDefaultHome(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 29) try {
            val rm = ctx.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) return rm.isRoleHeld(RoleManager.ROLE_HOME)
        } catch (e: RuntimeException) { /* some vendor builds strip RoleManager — fall through */ }
        val res = ctx.packageManager.resolveActivity(homeIntent(), PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == ctx.packageName
    }

    /** Asks the system to make DriveLite the Home app (system dialog), else opens Home settings. */
    fun requestDefaultHome(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 29) try {
            val rm = activity.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                @Suppress("DEPRECATION")
                activity.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME_ROLE)
                return
            }
        } catch (e: RuntimeException) { /* fall back below */ }
        openHomeSettings(activity)
    }

    /**
     * "Return to Original Home": opens the system screen where the default Home app is chosen.
     * Tries the most specific screen first; if the head unit hides it, shows manual steps.
     */
    fun openHomeSettings(activity: Activity) {
        val specific = listOf(Settings.ACTION_HOME_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        for (action in specific) {
            if (tryStart(activity, Intent(action))) return
        }
        showManualSteps(activity, openSettingsAfter = true)
    }

    fun showManualSteps(activity: Activity, openSettingsAfter: Boolean = false) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.manual_title)
            .setMessage(R.string.manual_msg)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (openSettingsAfter) tryStart(activity, Intent(Settings.ACTION_SETTINGS))
            }
            .show()
    }

    /** Other installed Home apps (e.g. the manufacturer launcher), excluding Android's FallbackHome. */
    fun otherHomes(ctx: Context): List<ResolveInfo> =
        ctx.packageManager.queryIntentActivities(homeIntent(), 0)
            .filter {
                val pkg = it.activityInfo.packageName
                pkg != ctx.packageName && pkg != "com.android.settings"
            }

    /** Opens another launcher right now (does not change the default). */
    fun launchHome(activity: Activity, info: ResolveInfo): Boolean {
        val intent = homeIntent()
            .setComponent(ComponentName(info.activityInfo.packageName, info.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return tryStart(activity, intent)
    }

    private fun tryStart(activity: Activity, intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
