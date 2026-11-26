package com.myrepo.oslauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(0xFFF2F2F7.toInt())
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(20), dp(28), dp(20), dp(28))
        scroll.addView(col)
        setContentView(scroll)

        fun title(text: String, size: Float = 22f, top: Int = 0): TextView {
            val t = TextView(this)
            t.text = text
            t.textSize = size
            t.setTextColor(0xFF1D1B20.toInt())
            t.setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.topMargin = dp(top)
            col.addView(t, lp)
            return t
        }

        title("Настройки лаунчера", 26f)
        title("Стиль", 18f, 20)

        val group = RadioGroup(this)
        group.orientation = RadioGroup.VERTICAL
        for (s in Style.values()) {
            val rb = RadioButton(this)
            rb.id = 1000 + s.ordinal
            rb.text = s.title + "\n" + s.desc
            rb.textSize = 15f
            rb.setTextColor(0xFF1D1B20.toInt())
            rb.setPadding(dp(8), dp(10), dp(8), dp(10))
            group.addView(rb, RadioGroup.LayoutParams(-1, -2))
        }
        group.check(1000 + prefs.style.ordinal)
        group.setOnCheckedChangeListener { _, id -> prefs.style = Style.values()[id - 1000] }
        col.addView(group)

        title("Вид", 18f, 20)

        val labels = Switch(this)
        labels.text = "Подписи под иконками"
        labels.isChecked = prefs.showLabels
        labels.setOnCheckedChangeListener { _, v -> prefs.showLabels = v }
        col.addView(labels, LinearLayout.LayoutParams(-1, dp(52)))

        val dots = Switch(this)
        dots.text = "Точки страниц"
        dots.isChecked = prefs.showDots
        dots.setOnCheckedChangeListener { _, v -> prefs.showDots = v }
        col.addView(dots, LinearLayout.LayoutParams(-1, dp(52)))

        val sizeLabel = TextView(this)
        sizeLabel.textSize = 15f
        sizeLabel.setTextColor(0xFF1D1B20.toInt())
        sizeLabel.text = "Размер иконок: " + prefs.iconScale + "%"
        col.addView(sizeLabel)
        val seek = SeekBar(this)
        seek.max = 60
        seek.progress = prefs.iconScale - 70
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                prefs.iconScale = 70 + p
                sizeLabel.text = "Размер иконок: " + (70 + p) + "%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        col.addView(seek, LinearLayout.LayoutParams(-1, -2))

        title("Прочее", 18f, 20)

        val home = Button(this)
        home.text = "Сделать лаунчером по умолчанию"
        home.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
        col.addView(home, LinearLayout.LayoutParams(-1, -2))

        val resetDock = Button(this)
        resetDock.text = "Сбросить док"
        resetDock.setOnClickListener { prefs.dock = emptyList() }
        col.addView(resetDock, LinearLayout.LayoutParams(-1, -2))

        val hint = TextView(this)
        hint.text = "Изменения применяются, когда ты вернёшься на главный экран. " +
            "Чтобы открыть эти настройки с главного экрана, зажми пустое место."
        hint.textSize = 13f
        hint.setTextColor(Color.GRAY)
        val hlp = LinearLayout.LayoutParams(-1, -2)
        hlp.topMargin = dp(16)
        col.addView(hint, hlp)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
}
