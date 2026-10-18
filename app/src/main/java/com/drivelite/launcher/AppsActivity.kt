package com.drivelite.launcher

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView

/** All-apps grid. Views are recycled; icons load lazily off the UI thread and are cached. */
class AppsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var launcher: AppLauncher
    private lateinit var palette: DriveTheme
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apps)
        prefs = Prefs(this)
        launcher = AppLauncher(this, prefs)
        palette = prefs.theme

        Ui.paintWindow(this, palette)
        findViewById<View>(R.id.root).setBackgroundColor(palette.bg)
        findViewById<TextView>(R.id.header_title).apply { setText(R.string.apps_title); setTextColor(palette.text) }
        findViewById<ImageView>(R.id.btn_back).apply { setColorFilter(palette.text); setOnClickListener { finish() } }
        findViewById<TextView>(R.id.empty).setTextColor(palette.textDim)

        findViewById<GridView>(R.id.grid).apply {
            adapter = this@AppsActivity.adapter
            setOnItemClickListener { _, _, pos, _ -> launcher.openApp(this@AppsActivity.adapter.items[pos]) }
        }
    }

    override fun onResume() {
        super.onResume()
        AppsRepository.loadAsync(this) { list ->
            if (isFinishing || isDestroyed) return@loadAsync
            adapter.items = list
            adapter.notifyDataSetChanged()
            findViewById<TextView>(R.id.empty).apply {
                setText(R.string.apps_empty)
                visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private inner class Adapter : BaseAdapter() {
        var items: List<AppEntry> = emptyList()

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false).also {
                it.background = Ui.pressable(it, palette.surface, palette.accent.withAlpha(70), 16f)
                it.findViewById<TextView>(R.id.app_label).setTextColor(palette.text)
            }
            val entry = items[position]
            v.findViewById<TextView>(R.id.app_label).text = entry.label
            val icon = v.findViewById<ImageView>(R.id.app_icon)
            icon.tag = entry.component
            val cached = AppsRepository.cachedIcon(entry.component)
            if (cached != null) {
                icon.setImageDrawable(cached)
            } else {
                icon.setImageDrawable(null)
                AppsRepository.loadIconAsync(this@AppsActivity, entry.component) { d ->
                    if (icon.tag == entry.component) icon.setImageDrawable(d) // view not recycled meanwhile
                }
            }
            return v
        }
    }
}
