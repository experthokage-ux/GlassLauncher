package com.fares.glass.gfx

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.random.Random

enum class SkyPhase { DAWN, DAY, DUSK, NIGHT }

/** Time-of-day skies: sunrise, day, sunset, moonlit night. Generated in code like [Wallpapers]. */
object SkyWallpaper {

    private class Sky(
        val colors: IntArray, val stops: FloatArray,
        val bodyX: Float, val bodyY: Float, val bodyR: Float, val bodyColor: Int,
        val cloud: Int,
    )

    private val skies = mapOf(
        SkyPhase.DAWN to Sky(
            intArrayOf(0xFF2E3B7A.toInt(), 0xFFB9639B.toInt(), 0xFFFF9A6B.toInt(), 0xFFFFD98A.toInt()),
            floatArrayOf(0f, 0.45f, 0.78f, 1f),
            0.68f, 0.74f, 62f, 0xFFFFF1B8.toInt(), 0xFFFFC9A8.toInt()),
        SkyPhase.DAY to Sky(
            intArrayOf(0xFF1F6FD1.toInt(), 0xFF5BA8F0.toInt(), 0xFFBFE3FF.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            0.80f, 0.20f, 58f, 0xFFFFF8D6.toInt(), 0xFFFFFFFF.toInt()),
        SkyPhase.DUSK to Sky(
            intArrayOf(0xFF1B1F4B.toInt(), 0xFF7B3F8F.toInt(), 0xFFE8556B.toInt(), 0xFFFF9A4D.toInt()),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
            0.30f, 0.76f, 66f, 0xFFFFB067.toInt(), 0xFFFFA08A.toInt()),
        SkyPhase.NIGHT to Sky(
            intArrayOf(0xFF03060F.toInt(), 0xFF0A1330.toInt(), 0xFF16224A.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            0.78f, 0.24f, 64f, 0xFFF4F1E6.toInt(), 0xFF5A6A94.toInt()),
    )

    /** Which sky to show, given sunrise/sunset (epoch millis). */
    fun phase(now: Long, sunrise: Long, sunset: Long): SkyPhase {
        val min = 60_000L
        return when {
            now in (sunrise - 45 * min)..(sunrise + 60 * min) -> SkyPhase.DAWN
            now in (sunset - 60 * min)..(sunset + 45 * min) -> SkyPhase.DUSK
            now > sunrise && now < sunset -> SkyPhase.DAY
            else -> SkyPhase.NIGHT
        }
    }

    fun render(phase: SkyPhase, overcast: Boolean, w: Int = 1280, h: Int = 720): Bitmap {
        val sky = skies.getValue(phase)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(phase.ordinal * 101 + 7)

        // Sky gradient.
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), sky.colors, sky.stops, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        // Stars (clear nights only).
        if (phase == SkyPhase.NIGHT && !overcast) {
            repeat(170) {
                val r = 0.6f + rnd.nextFloat() * 1.4f
                p.color = (((90 + rnd.nextInt(165)) shl 24) or 0x00FFFFFF)
                c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.7f, r, p)
            }
        }

        // Sun / moon with glow.
        val bx = w * sky.bodyX
        val by = h * sky.bodyY
        val glowAlpha = if (overcast) 0x40 else 0x99
        p.shader = RadialGradient(bx, by, sky.bodyR * 6f,
            intArrayOf(alpha(sky.bodyColor, glowAlpha), alpha(sky.bodyColor, 0x30), alpha(sky.bodyColor, 0)),
            floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(bx, by, sky.bodyR * 6f, p)
        p.shader = null
        if (!overcast) {
            p.color = sky.bodyColor
            c.drawCircle(bx, by, sky.bodyR, p)
            if (phase == SkyPhase.NIGHT) { // faint craters
                p.color = alpha(0xFFB8B5A8.toInt(), 0x60)
                c.drawCircle(bx - sky.bodyR * 0.3f, by - sky.bodyR * 0.2f, sky.bodyR * 0.22f, p)
                c.drawCircle(bx + sky.bodyR * 0.35f, by + sky.bodyR * 0.25f, sky.bodyR * 0.16f, p)
                c.drawCircle(bx + sky.bodyR * 0.1f, by - sky.bodyR * 0.5f, sky.bodyR * 0.12f, p)
            }
        }

        // Overcast: dim and grey the whole sky.
        if (overcast) {
            p.color = if (phase == SkyPhase.NIGHT) alpha(0xFF05070C.toInt(), 0x99) else alpha(0xFF3A4150.toInt(), 0xA8)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        }

        // Soft clouds.
        p.maskFilter = BlurMaskFilter(42f, BlurMaskFilter.Blur.NORMAL)
        val count = if (overcast) 10 else 5
        val cloudAlpha = if (overcast) 0x70 else 0x48
        val tint = if (overcast) 0xFF8E96A6.toInt() else sky.cloud
        p.color = alpha(tint, cloudAlpha)
        repeat(count) {
            val cx = rnd.nextFloat() * w
            val cy = h * (0.10f + rnd.nextFloat() * 0.55f)
            val cw = w * (0.12f + rnd.nextFloat() * 0.14f)
            repeat(4) { j ->
                val ox = (j - 1.5f) * cw * 0.45f
                val oy = (rnd.nextFloat() - 0.5f) * cw * 0.2f
                val rw = cw * (0.45f + rnd.nextFloat() * 0.3f)
                val rh = rw * 0.45f
                c.drawOval(RectF(cx + ox - rw, cy + oy - rh, cx + ox + rw, cy + oy + rh), p)
            }
        }
        p.maskFilter = null

        // Light grain against gradient banding.
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val noise = Random(phase.ordinal + 1)
        for (i in px.indices) {
            val n = noise.nextInt(-3, 4)
            val v = px[i]
            val r = ((v shr 16) and 0xFF) + n
            val g = ((v shr 8) and 0xFF) + n
            val b = (v and 0xFF) + n
            px[i] = (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)
}
