package com.careq

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.min

/** Большая неоновая ручка BASS (0..1). */
class BassKnob(ctx: Context) : View(ctx) {
    private var value = 0f
    var onChange: ((Float) -> Unit)? = null
    private val start = 135f
    private val sweep = 270f
    private val rect = RectF()
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = 0x33FFFFFF }
    private val prog = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF4DA6"); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val core = Paint(Paint.ANTI_ALIAS_FLAG)

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    fun setValue(v: Float) { value = v.coerceIn(0f, 1f); invalidate() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val size = min(w, h)
        val sw = size * 0.09f
        val r = size / 2f - sw * 1.6f
        val cx = w / 2f; val cy = h / 2f
        rect.set(cx - r, cy - r, cx + r, cy + r)

        // подсветка центра
        core.shader = RadialGradient(cx, cy, r, intArrayOf(
            Color.argb((40 + 120 * value).toInt(), 255, 60, 140), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, core)

        track.strokeWidth = sw
        c.drawArc(rect, start, sweep, false, track)

        if (value > 0.001f) {
            c.save()
            c.rotate(start, cx, cy)
            prog.strokeWidth = sw
            prog.shader = SweepGradient(cx, cy,
                intArrayOf(Color.parseColor("#00E5FF"), Color.parseColor("#B14DFF"), Color.parseColor("#FF2D75"), Color.parseColor("#FF9A00")),
                floatArrayOf(0f, 0.3f, 0.55f, 0.75f))
            prog.setShadowLayer(sw * 0.9f, 0f, 0f, Color.parseColor("#FF2D75"))
            c.drawArc(rect, 0f, sweep * value, false, prog)
            c.restore()
        }

        small.textSize = size * 0.09f
        big.textSize = size * 0.22f
        c.drawText("BASS", cx, cy - size * 0.06f, small)
        c.drawText("${(value * 100).toInt()}%", cx, cy + size * 0.12f, big)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val dx = e.x - width / 2f
            val dy = e.y - height / 2f
            var ang = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
            if (ang < 0) ang += 360f
            var rel = ang - start
            if (rel < 0) rel += 360f
            val v = if (rel <= sweep) rel / sweep else if (rel - sweep < (360f - sweep) / 2f) 1f else 0f
            value = v
            onChange?.invoke(v)
            invalidate()
        }
        return true
    }
}
