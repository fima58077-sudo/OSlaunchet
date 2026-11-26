package com.myrepo.oslauncher

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var style: Style
        get() = Style.values().getOrElse(sp.getInt("style", 0)) { Style.IOS }
        set(v) { sp.edit().putInt("style", v.ordinal).apply() }

    var showLabels: Boolean
        get() = sp.getBoolean("labels", true)
        set(v) { sp.edit().putBoolean("labels", v).apply() }

    var showDots: Boolean
        get() = sp.getBoolean("dots", true)
        set(v) { sp.edit().putBoolean("dots", v).apply() }

    /** Размер иконок в процентах, 70..130 */
    var iconScale: Int
        get() = sp.getInt("scale", 100)
        set(v) { sp.edit().putInt("scale", v).apply() }

    var dock: List<String>
        get() = (sp.getString("dock", "") ?: "").split(",").filter { it.isNotBlank() }
        set(v) { sp.edit().putString("dock", v.joinToString(",")).apply() }
}
