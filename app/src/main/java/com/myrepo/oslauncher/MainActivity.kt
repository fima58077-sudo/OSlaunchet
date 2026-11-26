package com.myrepo.oslauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var root: FrameLayout
    private var apps: List<AppItem> = emptyList()
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        root = FrameLayout(this)
        setContentView(root)
        WindowCompat.getInsetsController(window, root).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        Thread {
            val list = AppRepo.load(this)
            runOnUiThread {
                apps = list
                render()
            }
        }.start()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // лаунчер не закрывается
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        currentPage = 0
        render()
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    // ---------- сборка экрана ----------

    private fun render() {
        val style = prefs.style
        val spec = style.spec()
        val scale = prefs.iconScale / 100f

        root.removeAllViews()
        root.setBackgroundColor(spec.scrim)

        val dockPkgs = if (spec.dock) ensureDock(spec) else emptyList()
        val gridApps = apps.filter { it.pkg !in dockPkgs }

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        if (spec.menuBar) column.addView(menuBar())
        if (spec.tvBar) column.addView(tvBar())

        val pages = gridApps.chunked(spec.cols * spec.rows)
        val pager = ViewPager2(this)
        pager.adapter = PagesAdapter(pages, spec, scale)

        val pagerHolder = FrameLayout(this)
        pagerHolder.addView(pager, FrameLayout.LayoutParams(-1, -1))
        val holderLp = LinearLayout.LayoutParams(-1, 0, 1f)
        if (spec.glass) {
            val panel = GradientDrawable()
            panel.cornerRadius = dpf(36f)
            panel.setColor(0x2EFFFFFF)
            panel.setStroke(dp(1), 0x55FFFFFF)
            pagerHolder.background = panel
            holderLp.setMargins(dp(14), dp(8), dp(14), dp(8))
        }
        column.addView(pagerHolder, holderLp)

        val showDots = spec.dots && prefs.showDots && pages.size > 1
        val dotsRow = LinearLayout(this)
        dotsRow.gravity = Gravity.CENTER
        if (showDots) {
            pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentPage = position
                    updateDots(dotsRow, pages.size, position)
                }
            })
            updateDots(dotsRow, pages.size, currentPage.coerceAtMost(pages.size - 1))
            column.addView(dotsRow, LinearLayout.LayoutParams(-1, dp(24)))
        }
        if (pages.isNotEmpty()) pager.setCurrentItem(currentPage.coerceAtMost(pages.size - 1), false)

        if (spec.dock) {
            val dock = dockView(dockPkgs, spec, scale)
            val lp = LinearLayout.LayoutParams(-2, -2)
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.setMargins(dp(12), dp(4), dp(12), dp(10))
            column.addView(dock, lp)
        }
    }

    private fun updateDots(row: LinearLayout, count: Int, selected: Int) {
        row.removeAllViews()
        for (i in 0 until count) {
            val d = View(this)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(Color.WHITE)
            d.background = g
            d.alpha = if (i == selected) 1f else 0.4f
            val lp = LinearLayout.LayoutParams(dp(7), dp(7))
            lp.setMargins(dp(4), 0, dp(4), 0)
            row.addView(d, lp)
        }
    }

    private fun ensureDock(spec: Spec): List<String> {
        var d = prefs.dock.filter { p -> apps.any { it.pkg == p } }
        if (d.isEmpty() && apps.isNotEmpty()) {
            d = AppRepo.defaultDock(this, apps, spec.dockMax)
            prefs.dock = d
        }
        return d.take(spec.dockMax)
    }

    // ---------- панели ----------

    private fun menuBar(): View {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setBackgroundColor(0x55000000)
        bar.setPadding(dp(14), 0, dp(14), 0)

        val name = TextView(this)
        name.text = "Finder"
        name.setTextColor(Color.WHITE)
        name.textSize = 13f
        name.setTypeface(null, Typeface.BOLD)
        bar.addView(name)

        val space = Space(this)
        bar.addView(space, LinearLayout.LayoutParams(0, 1, 1f))

        val clock = TextClock(this)
        clock.format12Hour = "EEE H:mm"
        clock.format24Hour = "EEE H:mm"
        clock.setTextColor(Color.WHITE)
        clock.textSize = 13f
        bar.addView(clock)

        bar.layoutParams = LinearLayout.LayoutParams(-1, dp(28))
        return bar
    }

    private fun tvBar(): View {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(18), dp(10), dp(18), dp(6))

        fun tab(text: String, bold: Boolean, click: (() -> Unit)?) {
            val t = TextView(this)
            t.text = text
            t.textSize = 17f
            t.setTextColor(if (bold) Color.WHITE else 0xAAFFFFFF.toInt())
            if (bold) t.setTypeface(null, Typeface.BOLD)
            t.setPadding(0, 0, dp(18), 0)
            if (click != null) t.setOnClickListener { click() }
            bar.addView(t)
        }
        tab("Главная", true, null)
        tab("Библиотека", false, null)
        tab("Настройки", false) { openSettings() }

        val space = Space(this)
        bar.addView(space, LinearLayout.LayoutParams(0, 1, 1f))

        val clock = TextClock(this)
        clock.format12Hour = "H:mm"
        clock.format24Hour = "H:mm"
        clock.setTextColor(Color.WHITE)
        clock.textSize = 17f
        bar.addView(clock)
        return bar
    }

    private fun dockView(pkgs: List<String>, spec: Spec, scale: Float): View {
        val dock = LinearLayout(this)
        dock.orientation = LinearLayout.HORIZONTAL
        dock.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.cornerRadius = dpf(spec.dockRadius.toFloat())
        bg.setColor(spec.dockColor)
        bg.setStroke(dp(1), spec.dockStroke)
        dock.background = bg
        dock.setPadding(dp(14), dp(10), dp(14), dp(10))

        val size = fitSize(dp((spec.dockIcon * scale).toInt()), spec.dockMax + 1)
        for (p in pkgs) {
            val app = apps.firstOrNull { it.pkg == p } ?: continue
            val icon = iconView(app.icon, spec, size, size)
            icon.setOnClickListener { launch(app) }
            icon.setOnLongClickListener { showMenu(app); true }
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(dp(6), 0, dp(6), 0)
            dock.addView(icon, lp)
        }
        return dock
    }

    // ---------- страницы ----------

    private class PageHolder(val page: LinearLayout) : RecyclerView.ViewHolder(page)

    private inner class PagesAdapter(
        val pages: List<List<AppItem>>,
        val spec: Spec,
        val scale: Float
    ) : RecyclerView.Adapter<PageHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val page = LinearLayout(this@MainActivity)
            page.orientation = LinearLayout.VERTICAL
            page.layoutParams = ViewGroup.LayoutParams(-1, -1)
            page.setOnLongClickListener { openSettings(); true }
            return PageHolder(page)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            buildPage(holder.page, pages[position], spec, scale)
        }

        override fun getItemCount(): Int = pages.size
    }

    private fun buildPage(page: LinearLayout, items: List<AppItem>, spec: Spec, scale: Float) {
        page.removeAllViews()
        for (r in 0 until spec.rows) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (c in 0 until spec.cols) {
                val idx = r * spec.cols + c
                val cell: View = if (idx < items.size) appCell(items[idx], spec, scale) else Space(this)
                row.addView(cell, LinearLayout.LayoutParams(0, -1, 1f))
            }
            page.addView(row, LinearLayout.LayoutParams(-1, 0, 1f))
        }
    }

    private fun fitSize(px: Int, cols: Int): Int {
        val max = (resources.displayMetrics.widthPixels / cols * 0.86f).toInt()
        return if (px > max) max else px
    }

    private fun appCell(app: AppItem, spec: Spec, scale: Float): View {
        val s = if (spec.shape == Shape.CARD && scale > 1.1f) 1.1f else scale
        val h = fitSize(dp((spec.icon * s).toInt()), spec.cols)
        val w = if (spec.shape == Shape.CARD) fitSize((h * 1.7f).toInt(), spec.cols) else h
        val hh = if (spec.shape == Shape.CARD) (w / 1.7f).toInt() else h

        val cell = LinearLayout(this)
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER
        cell.addView(iconView(app.icon, spec, w, hh), LinearLayout.LayoutParams(w, hh))

        if (prefs.showLabels) {
            val label = TextView(this)
            label.text = app.label
            label.textSize = spec.label.toFloat()
            label.setTextColor(Color.WHITE)
            label.setShadowLayer(4f, 0f, 1f, 0x99000000.toInt())
            label.maxLines = 1
            label.ellipsize = TextUtils.TruncateAt.END
            label.gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.topMargin = dp(5)
            lp.leftMargin = dp(2)
            lp.rightMargin = dp(2)
            cell.addView(label, lp)
        }
        cell.setOnClickListener { launch(app) }
        cell.setOnLongClickListener { showMenu(app); true }
        return cell
    }

    // ---------- иконка в нужной форме ----------

    private fun iconView(d: Drawable, spec: Spec, w: Int, h: Int): FrameLayout {
        val f = FrameLayout(this)
        f.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                when (spec.shape) {
                    Shape.CIRCLE -> outline.setOval(0, 0, view.width, view.height)
                    Shape.CARD -> outline.setRoundRect(0, 0, view.width, view.height, view.height * 0.14f)
                    Shape.SQUIRCLE -> outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.225f)
                }
            }
        }
        f.clipToOutline = true
        if (spec.shadow) f.elevation = dpf(5f)

        val img = ImageView(this)
        img.setImageDrawable(d)
        img.scaleType = ImageView.ScaleType.CENTER_CROP

        when (spec.shape) {
            Shape.CARD -> {
                val bg = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(0xFF4A4F73.toInt(), 0xFF1B1D2E.toInt())
                )
                f.background = bg
                val inner = (h * 0.62f).toInt()
                img.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.225f)
                    }
                }
                img.clipToOutline = true
                val lp = FrameLayout.LayoutParams(inner, inner)
                lp.gravity = Gravity.CENTER
                f.addView(img, lp)
            }
            Shape.CIRCLE -> {
                val glass = GradientDrawable()
                glass.shape = GradientDrawable.OVAL
                glass.setColor(0x33FFFFFF)
                f.background = glass
                f.addView(img, FrameLayout.LayoutParams(-1, -1))
                val ring = GradientDrawable()
                ring.shape = GradientDrawable.OVAL
                ring.setStroke(dp(1), 0x77FFFFFF)
                ring.setColor(Color.TRANSPARENT)
                f.foreground = ring
            }
            Shape.SQUIRCLE -> {
                f.background = ColorDrawable(0xFFF2F2F7.toInt())
                f.addView(img, FrameLayout.LayoutParams(-1, -1))
            }
        }
        return f
    }

    // ---------- действия ----------

    private fun launch(app: AppItem) {
        try {
            val i = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(app.component.packageName, app.component.className))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть " + app.label, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMenu(app: AppItem) {
        val spec = prefs.style.spec()
        val inDock = prefs.dock.contains(app.pkg)
        val items = ArrayList<String>()
        items.add("О приложении")
        items.add("Удалить")
        if (spec.dock) items.add(if (inDock) "Убрать из дока" else "Добавить в док")
        items.add("Настройки лаунчера")
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(items.toTypedArray()) { _, which ->
                when (items[which]) {
                    "О приложении" -> startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.pkg))
                    )
                    "Удалить" -> startActivity(
                        Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.pkg))
                    )
                    "Убрать из дока" -> {
                        prefs.dock = prefs.dock.filter { it != app.pkg }
                        render()
                    }
                    "Добавить в док" -> {
                        val cur = prefs.dock
                        if (cur.size >= spec.dockMax) {
                            Toast.makeText(this, "Док заполнен", Toast.LENGTH_SHORT).show()
                        } else {
                            prefs.dock = cur + app.pkg
                            render()
                        }
                    }
                    else -> openSettings()
                }
            }
            .show()
    }
}
