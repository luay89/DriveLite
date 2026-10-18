package com.drivelite.launcher

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.TextView

/** Small helpers to paint framework Views with the active theme (no styles per theme needed). */
object Ui {

    fun dp(view: View, value: Float): Float = value * view.resources.displayMetrics.density

    fun rounded(color: Int, radiusPx: Float, strokeColor: Int? = null, strokePx: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(color)
            if (strokeColor != null && strokePx > 0) setStroke(strokePx, strokeColor)
        }

    /** Rounded surface with a touch ripple — cheap, hardware-accelerated feedback. */
    fun pressable(view: View, fill: Int, ripple: Int, radiusDp: Float = 18f, stroke: Int? = null): Drawable {
        val r = dp(view, radiusDp)
        val content = rounded(fill, r, stroke, if (stroke != null) dp(view, 2f).toInt() else 0)
        val mask = rounded(0xFFFFFFFF.toInt(), r)
        return RippleDrawable(ColorStateList.valueOf(ripple), content, mask)
    }

    fun styleButton(button: TextView, t: DriveTheme) {
        button.background = pressable(button, t.surface, t.accent.withAlpha(70), 14f)
        button.setTextColor(t.text)
    }

    @Suppress("DEPRECATION") // still the simplest way on API 31–34 without AndroidX
    fun paintWindow(activity: Activity, t: DriveTheme) {
        activity.window.decorView.setBackgroundColor(t.bg)
        activity.window.statusBarColor = t.bg
        activity.window.navigationBarColor = t.bg
    }
}
