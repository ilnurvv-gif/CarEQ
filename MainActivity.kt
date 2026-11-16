package com.careq

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.core.content.ContextCompat
import kotlin.math.min

class MainActivity : Activity() {
    private val bandBars = ArrayList<SeekBar>()
    private lateinit var knob: BassKnob
    private lateinit var virtBar: SeekBar
    private lateinit var loudBar: SeekBar
    private lateinit var subBtn: Button
    private var subOn = false

    private val colors = intArrayOf(
        Color.parseColor("#00E5FF"), Color.parseColor("#3DDC97"), Color.parseColor("#C6FF00"),
        Color.parseColor("#FFC400"), Color.parseColor("#FF6D00"), Color.parseColor("#FF2D75"),
        Color.parseColor("#B14DFF"), Color.parseColor("#448AFF")
    )

    // пресет: форма полос (-1..1) и сила баса (0..1000)
    private val presets = linkedMapOf(
        "Плоский" to Pair(floatArrayOf(0f, 0f, 0f, 0f, 0f), 0),
        "Бас+" to Pair(floatArrayOf(1f, 0.7f, 0f, 0f, 0.1f), 800),
        "Рок" to Pair(floatArrayOf(0.6f, 0.3f, -0.2f, 0.4f, 0.6f), 300),
        "Поп" to Pair(floatArrayOf(-0.2f, 0.3f, 0.5f, 0.3f, -0.2f), 200),
        "Голос" to Pair(floatArrayOf(-0.3f, 0f, 0.6f, 0.4f, 0f), 0),
        "Клуб" to Pair(floatArrayOf(0.9f, 0.5f, 0.1f, 0.5f, 0.8f), 650)
    )

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ContextCompat.startForegroundService(this, Intent(this, EqService::class.java))
        EqEngine.init(this)
        buildUi()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(fill: Int, r: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(r).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    private fun label(t: String, size: Float = 16f, color: Int = Color.WHITE) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(0x22FFFFFF, 22, 0x33FFFFFF)
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }

    private fun lp(w: Int = -1, h: Int = -2, top: Int = 0, bottom: Int = 0, side: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top); bottomMargin = dp(bottom); leftMargin = dp(side); rightMargin = dp(side) }

    private fun buildUi() {
        val dm = resources.displayMetrics
        val land = dm.widthPixels > dm.heightPixels
        val main = LinearLayout(this).apply {
            orientation = if (land) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        val sv = ScrollView(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#070B24"), Color.parseColor("#2B0A42"), Color.parseColor("#0A1B3A")))
            isFillViewport = true
            addView(main)
        }
        setContentView(sv)
        val ks = if (land) min(dp(300), (dm.heightPixels * 0.55f).toInt()) else (dm.widthPixels * 0.7f).toInt()
        main.addView(leftPanel(ks), if (land) LinearLayout.LayoutParams(0, -2, 0.85f).apply { rightMargin = dp(12) } else lp(bottom = 12))
        main.addView(rightPanel(), if (land) LinearLayout.LayoutParams(0, -2, 1.15f) else lp())
    }

    private fun leftPanel(ks: Int): LinearLayout {
        val p = card().apply { gravity = Gravity.CENTER_HORIZONTAL }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("CAR EQ", 28f, Color.parseColor("#00E5FF")).apply { typeface = Typeface.DEFAULT_BOLD },
            LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(Switch(this).apply {
            isChecked = EqEngine.isOn(this@MainActivity)
            scaleX = 1.4f; scaleY = 1.4f
            setOnCheckedChangeListener { _, on -> EqEngine.setEnabled(this@MainActivity, on) }
        })
        p.addView(top, lp())

        knob = BassKnob(this).apply {
            setValue(EqEngine.get(this@MainActivity, "bass") / 1000f)
            onChange = { EqEngine.setBass(this@MainActivity, (it * 1000).toInt()) }
        }
        p.addView(knob, lp(ks, ks, top = 10, bottom = 10))

        subBtn = Button(this).apply {
            isAllCaps = false; textSize = 20f; setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { applySub(!subOn) }
        }
        styleSub()
        p.addView(subBtn, lp(h = dp(68)))
        p.addView(label("SUB: макс. бас, низкие полосы вверх, +громкость", 12f, Color.parseColor("#AAB4D4")),
            lp(top = 6))
        return p
    }

    private fun rightPanel(): LinearLayout {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        EqEngine.error?.let {
            col.addView(label("⚠ $it\nНа этой магнитоле глобальный эффект может не поддерживаться.", 13f, Color.parseColor("#FFB020")), lp(bottom = 8))
        }

        // пресеты
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this)
        var ci = 0
        presets.forEach { (name, pr) ->
            val col1 = colors[(ci * 2 + 1) % colors.size]; ci++
            row.addView(Button(this).apply {
                text = name; isAllCaps = false; textSize = 16f
                setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
                background = rounded(col1, 16)
                setOnClickListener { applyPreset(pr.first, pr.second) }
            }, LinearLayout.LayoutParams(-2, dp(52)).apply { rightMargin = dp(8) })
        }
        hs.addView(row)
        col.addView(hs, lp(bottom = 10))

        // полосы
        val bands = card()
        bands.addView(label("ЭКВАЛАЙЗЕР", 14f, Color.parseColor("#AAB4D4")))
        val eq = EqEngine.eq
        if (eq != null) {
            val min = eq.bandLevelRange[0].toInt()
            val max = eq.bandLevelRange[1].toInt()
            for (i in 0 until eq.numberOfBands) {
                val hz = eq.getCenterFreq(i.toShort()) / 1000
                val name = if (hz >= 1000) "${hz / 1000} кГц" else "$hz Гц"
                val c = colors[i % colors.size]
                val tv = label(name, 15f, c).apply { typeface = Typeface.DEFAULT_BOLD }
                bands.addView(tv, lp(top = 6))
                val bar = seek(max - min, EqEngine.get(this, "band$i").coerceIn(min, max) - min, c) {
                    val lvl = it + min
                    tv.text = "$name   ${"%+.1f".format(lvl / 100f)} дБ"
                    EqEngine.setBand(this, i, lvl)
                }
                bandBars.add(bar)
                bands.addView(bar)
            }
        } else bands.addView(label("Эквалайзер недоступен на этом устройстве"))
        col.addView(bands, lp(bottom = 10))

        // доп.
        val ex = card()
        ex.addView(label("Объём (Virtualizer)", 15f, Color.parseColor("#B14DFF")).apply { typeface = Typeface.DEFAULT_BOLD })
        virtBar = seek(1000, EqEngine.get(this, "virt"), Color.parseColor("#B14DFF")) { EqEngine.setVirt(this, it) }
        ex.addView(virtBar)
        ex.addView(label("Громкость + (осторожно, может хрипеть)", 15f, Color.parseColor("#FF9A00")).apply { typeface = Typeface.DEFAULT_BOLD }, lp(top = 8))
        loudBar = seek(2000, EqEngine.get(this, "loud"), Color.parseColor("#FF9A00")) { EqEngine.setLoud(this, it) }
        ex.addView(loudBar)
        col.addView(ex)
        return col
    }

    private fun seek(max: Int, value: Int, color: Int, onChange: (Int) -> Unit) = SeekBar(this).apply {
        this.max = max; progress = value
        minimumHeight = dp(52)
        setPadding(dp(14), dp(10), dp(14), dp(10))
        progressTintList = ColorStateList.valueOf(color)
        thumbTintList = ColorStateList.valueOf(color)
        progressBackgroundTintList = ColorStateList.valueOf(0x44FFFFFF)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, user: Boolean) { onChange(p) }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        onChange(value)
    }

    private fun styleSub() {
        subBtn.text = if (subOn) "🔊 SUB МОД: ВКЛ" else "SUB МОД: ВЫКЛ"
        subBtn.background = if (subOn)
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#FF2D75"), Color.parseColor("#FF9A00"))).apply { cornerRadius = dp(20).toFloat() }
        else rounded(0x33FFFFFF, 20, 0x55FFFFFF)
    }

    private fun lvl(x: Float, min: Int, max: Int) = (if (x >= 0) x * max else -x * min).toInt().coerceIn(min, max)

    /** «Сабвуфер»: максимум баса, низкие полосы вверх, верх чуть вниз (меньше хрипа), + громкость. */
    private fun applySub(on: Boolean) {
        subOn = on
        styleSub()
        val eq = EqEngine.eq
        if (eq != null) {
            val min = eq.bandLevelRange[0].toInt()
            val max = eq.bandLevelRange[1].toInt()
            for (i in bandBars.indices) {
                val x = if (!on) 0f else when (i) { 0 -> 1f; 1 -> 0.75f; 2 -> 0.25f; else -> -0.15f }
                bandBars[i].progress = lvl(x, min, max) - min
            }
        }
        knob.setValue(if (on) 1f else 0f)
        EqEngine.setBass(this, if (on) 1000 else 0)
        loudBar.progress = if (on) 700 else 0
    }

    private fun applyPreset(v: FloatArray, bass: Int) {
        subOn = false; styleSub()
        val eq = EqEngine.eq ?: return
        val min = eq.bandLevelRange[0].toInt()
        val max = eq.bandLevelRange[1].toInt()
        val n = bandBars.size
        for (i in 0 until n) {
            val pos = if (n == 1) 0f else i * (v.size - 1f) / (n - 1)
            val a = pos.toInt().coerceAtMost(v.size - 2)
            val f = pos - a
            val x = v[a] * (1 - f) + v[a + 1] * f
            bandBars[i].progress = lvl(x, min, max) - min
        }
        knob.setValue(bass / 1000f)
        EqEngine.setBass(this, bass)
    }
}
