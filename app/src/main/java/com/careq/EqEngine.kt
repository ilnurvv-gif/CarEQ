package com.careq

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

/** Глобальные эффекты на аудио-сессии 0 (весь системный звук). */
object EqEngine {
    var eq: Equalizer? = null
    var bass: BassBoost? = null
    var virt: Virtualizer? = null
    var loud: LoudnessEnhancer? = null
    var error: String? = null

    private fun prefs(c: Context) = c.getSharedPreferences("eq", Context.MODE_PRIVATE)

    fun init(c: Context) {
        if (eq != null) return
        val p = prefs(c)
        try {
            eq = Equalizer(1000, 0)
            eq?.let { e ->
                for (i in 0 until e.numberOfBands) {
                    e.setBandLevel(i.toShort(), p.getInt("band$i", 0).toShort())
                }
            }
        } catch (t: Throwable) { error = "Equalizer: ${t.message}" }
        try { bass = BassBoost(1000, 0).also { it.setStrength(p.getInt("bass", 0).toShort()) } }
        catch (t: Throwable) { error = "BassBoost: ${t.message}" }
        try { virt = Virtualizer(1000, 0).also { it.setStrength(p.getInt("virt", 0).toShort()) } }
        catch (t: Throwable) { }
        try { loud = LoudnessEnhancer(0).also { it.setTargetGain(p.getInt("loud", 0)) } }
        catch (t: Throwable) { }
        setEnabled(c, p.getBoolean("on", true))
    }

    fun setEnabled(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("on", on).apply()
        try { eq?.enabled = on } catch (_: Throwable) {}
        try { bass?.enabled = on && (bass?.roundedStrength ?: 0) > 0 } catch (_: Throwable) {}
        try { virt?.enabled = on && (virt?.roundedStrength ?: 0) > 0 } catch (_: Throwable) {}
        try { loud?.enabled = on } catch (_: Throwable) {}
    }

    fun isOn(c: Context) = prefs(c).getBoolean("on", true)

    fun setBand(c: Context, i: Int, level: Int) {
        try { eq?.setBandLevel(i.toShort(), level.toShort()) } catch (_: Throwable) {}
        prefs(c).edit().putInt("band$i", level).apply()
    }

    fun setBass(c: Context, v: Int) {
        try { bass?.setStrength(v.toShort()); bass?.enabled = isOn(c) && v > 0 } catch (_: Throwable) {}
        prefs(c).edit().putInt("bass", v).apply()
    }

    fun setVirt(c: Context, v: Int) {
        try { virt?.setStrength(v.toShort()); virt?.enabled = isOn(c) && v > 0 } catch (_: Throwable) {}
        prefs(c).edit().putInt("virt", v).apply()
    }

    /** Громкость-буст в мБ (0..2000). Осторожно: может хрипеть. */
    fun setLoud(c: Context, mb: Int) {
        try { loud?.setTargetGain(mb); loud?.enabled = isOn(c) } catch (_: Throwable) {}
        prefs(c).edit().putInt("loud", mb).apply()
    }

    fun get(c: Context, key: String) = prefs(c).getInt(key, 0)
}
