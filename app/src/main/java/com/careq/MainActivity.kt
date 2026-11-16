package com.careq

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private val bandBars = ArrayList<SeekBar>()
    private lateinit var bassBar: SeekBar
    private lateinit var virtBar: SeekBar
    private lateinit var loudBar: SeekBar

    // пресеты: -1..1 от диапазона (5 точек, растягиваются на любое число полос)
    private val presets = linkedMapOf(
        "Плоский" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
        "Бас+" to floatArrayOf(0.9f, 0.6f, 0f, 0f, 0.1f),
        "Рок" to floatArrayOf(0.6f, 0.3f, -0.2f, 0.4f, 0.6f),
        "Поп" to floatArrayOf(-0.2f, 0.3f, 0.5f, 0.3f, -0.2f),
        "Голос" to floatArrayOf(-0.3f, 0f, 0.6f, 0.4f, 0f),
        "Клуб" to floatArrayOf(0.8f, 0.4f, 0.1f, 0.5f, 0.7f)
    )

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ContextCompat.startForegroundService(this, Intent(this, EqService::class.java))
        EqEngine.init(this)
        buildUi()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(t: String, size: Float = 18f) = TextView(this).apply {
        text = t; textSize = size; setTextColor(Color.WHITE)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(16))
            setBackgroundColor(Color.parseColor("#111418"))
        }
        val sv = ScrollView(this).apply { addView(root) }
        setContentView(sv)

        // заголовок + вкл/выкл
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("Car EQ", 26f), LinearLayout.LayoutParams(0, -2, 1f))
        val sw = Switch(this).apply {
            isChecked = EqEngine.isOn(this@MainActivity)
            scaleX = 1.5f; scaleY = 1.5f
            setOnCheckedChangeListener { _, on -> EqEngine.setEnabled(this@MainActivity, on) }
        }
        top.addView(sw)
        root.addView(top)

        EqEngine.error?.let {
            root.addView(label("⚠ $it\nНа этой магнитоле глобальный эффект может не поддерживаться.", 14f).apply { setTextColor(Color.parseColor("#FFB020")) })
        }

        // пресеты
        val hs = HorizontalScrollView(this)
        val row = LinearLayout(this)
        presets.forEach { (name, v) ->
            row.addView(Button(this).apply {
                text = name; isAllCaps = false; textSize = 16f
                setOnClickListener { applyPreset(v) }
            }, LinearLayout.LayoutParams(-2, dp(56)).apply { rightMargin = dp(8) })
        }
        hs.addView(row)
        root.addView(hs, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        // BASS BOOST
        root.addView(label("БАС БУСТ", 20f).apply { setTextColor(Color.parseColor("#FF5A36")) },
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(16) })
        bassBar = seek(1000, EqEngine.get(this, "bass")) { EqEngine.setBass(this, it) }
        root.addView(bassBar)

        // полосы
        val eq = EqEngine.eq
        if (eq != null) {
            val min = eq.bandLevelRange[0].toInt()
            val max = eq.bandLevelRange[1].toInt()
            for (i in 0 until eq.numberOfBands) {
                val hz = eq.getCenterFreq(i.toShort()) / 1000
                val name = if (hz >= 1000) "${hz / 1000} кГц" else "$hz Гц"
                val tv = label(name, 16f)
                root.addView(tv, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
                val bar = seek(max - min, EqEngine.get(this, "band$i").coerceIn(min, max) - min) {
                    val lvl = it + min
                    tv.text = "$name   ${"%+.1f".format(lvl / 100f)} дБ"
                    EqEngine.setBand(this, i, lvl)
                }
                bandBars.add(bar)
                root.addView(bar)
            }
        } else {
            root.addView(label("Эквалайзер недоступен на этом устройстве"))
        }

        // доп.
        root.addView(label("Объём (Virtualizer)", 16f), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(16) })
        virtBar = seek(1000, EqEngine.get(this, "virt")) { EqEngine.setVirt(this, it) }
        root.addView(virtBar)

        root.addView(label("Громкость + (осторожно, может хрипеть)", 16f), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
        loudBar = seek(2000, EqEngine.get(this, "loud")) { EqEngine.setLoud(this, it) }
        root.addView(loudBar)
    }

    private fun seek(max: Int, value: Int, onChange: (Int) -> Unit) = SeekBar(this).apply {
        this.max = max; progress = value
        minimumHeight = dp(56)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, user: Boolean) { onChange(p) }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        onChange(value)
    }

    private fun applyPreset(v: FloatArray) {
        val eq = EqEngine.eq ?: return
        val min = eq.bandLevelRange[0].toInt()
        val max = eq.bandLevelRange[1].toInt()
        val n = bandBars.size
        for (i in 0 until n) {
            val pos = if (n == 1) 0f else i * (v.size - 1f) / (n - 1)
            val a = pos.toInt().coerceAtMost(v.size - 2)
            val f = pos - a
            val x = v[a] * (1 - f) + v[a + 1] * f
            val lvl = if (x >= 0) (x * max).toInt() else (-x * min).toInt()
            bandBars[i].progress = lvl.coerceIn(min, max) - min
        }
        if (v === presets["Бас+"] || v === presets["Клуб"]) bassBar.progress = 700
        else if (v === presets["Плоский"]) bassBar.progress = 0
    }
}
