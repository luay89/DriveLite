package com.drivelite.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class AppEntry(val label: String, val component: ComponentName)

/**
 * Loads launchable apps and icons on ONE background thread so the UI thread never blocks
 * (the usual cause of launcher stutter on low-end head units).
 */
object AppsRepository {

    private val io: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "drivelite-io").apply { priority = Thread.NORM_PRIORITY - 1; isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())

    /** ~40 icons is plenty for one screen + scroll; bounded so memory stays flat. */
    private val iconCache = LruCache<String, Drawable>(60)

    fun loadLaunchable(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map {
                AppEntry(
                    it.loadLabel(pm).toString().trim(),
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                )
            }
            .distinctBy { it.component }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    fun loadAsync(ctx: Context, callback: (List<AppEntry>) -> Unit) {
        val app = ctx.applicationContext
        io.execute {
            val list = try { loadLaunchable(app) } catch (e: RuntimeException) { emptyList() }
            main.post { callback(list) }
        }
    }

    fun cachedIcon(component: ComponentName): Drawable? = iconCache.get(component.flattenToShortString())

    fun loadIconAsync(ctx: Context, component: ComponentName, callback: (Drawable?) -> Unit) {
        val app = ctx.applicationContext
        io.execute {
            val d = try {
                app.packageManager.getActivityIcon(component)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } catch (e: RuntimeException) {
                null
            }
            if (d != null) iconCache.put(component.flattenToShortString(), d)
            main.post { callback(d) }
        }
    }

    fun trimMemory() = iconCache.evictAll()
}
