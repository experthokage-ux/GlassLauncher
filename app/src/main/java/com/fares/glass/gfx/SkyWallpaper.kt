package com.fares.glass.gfx

import android.graphics.*
import kotlin.random.Random

enum class Sky { DAWN, DAY, DUSK, NIGHT }

object SkyWallpaper {
    private const val DUSK_WINDOW = 45 * 60_000L

    /** Uses real sunrise/sunset when known, otherwise fixed hours (Batangas is UTC+8). */
    fun phase(nowMs: Long, sunriseMs: Long?, sunsetMs: Long?): Sky {
        if (sunriseMs != null && sunsetMs != null) {
            return when {
                nowMs in (sunriseMs - DUSK_WINDOW)..(sunriseMs + DUSK_WINDOW) -> Sky.DAWN
                nowMs in (sunsetMs - DUSK_WINDOW)..(sunsetMs + DUSK_WINDOW) -> Sky.DUSK
                nowMs > sunriseMs && nowMs < sunsetMs -> Sky.DAY
                else -> Sky.NIGHT
            }
        }
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (h) { in 5..6 -> Sky.DAWN; in 7..16 -> Sky.DAY; in 17..18 -> Sky.DUSK; else -> Sky.NIGHT }
    }

    private fun c(v: Long) = v.toInt()

    fun render(sky: Sky, w: Int = 1920, h: Int = 1080, rain: Boolean = false): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val hf = h.toFloat(); val wf = w.toFloat()

        val (colors, pos) = when (sky) {
            Sky.DAWN -> intArrayOf(c(0xFF3A4A7D), c(0xFFE8869B), c(0xFFFFC38B)) to floatArrayOf(0f, .55f, 1f)
            Sky.DAY -> intArrayOf(c(0xFF1E78D6), c(0xFF5FB4F5), c(0xFFCDEBFF)) to floatArrayOf(0f, .55f, 1f)
            Sky.DUSK -> intArrayOf(c(0xFF1B1F52), c(0xFF8E3F7A), c(0xFFFF7A45), c(0xFFFFC46B)) to floatArrayOf(0f, .45f, .75f, 1f)
            Sky.NIGHT -> intArrayOf(c(0xFF03060F), c(0xFF0B1636), c(0xFF1C2D5C)) to floatArrayOf(0f, .6f, 1f)
        }
        p.shader = LinearGradient(0f, 0f, 0f, hf, colors, pos, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, wf, hf, p)
        p.shader = null

        when (sky) {
            Sky.DAWN -> {
                val sx = wf * .5f; val sy = hf * .74f
                glow(cv, sx, sy, hf * .9f, c(0xCCFFB27A))
                p.color = c(0xFFFFE2A8); cv.drawCircle(sx, sy, hf * .09f, p)
                clouds(cv, wf, hf, c(0x55FFC9B0), listOf(.12f to .35f, .7f to .3f, .4f to .5f, .85f to .55f))
            }
            Sky.DAY -> {
                val sx = wf * .78f; val sy = hf * .22f
                glow(cv, sx, sy, hf * .7f, c(0xCCFFF3B0))
                rays(cv, sx, sy, wf, hf)
                p.color = c(0xFFFFF6C2); cv.drawCircle(sx, sy, hf * .075f, p)
                clouds(cv, wf, hf, c(0x66FFFFFF), listOf(.15f to .28f, .45f to .16f, .62f to .38f, .3f to .55f))
            }
            Sky.DUSK -> {
                val sx = wf * .5f; val sy = hf * .72f
                glow(cv, sx, sy, hf * .9f, c(0xCCFF9A4D))
                p.color = c(0xFFFFD08A); cv.drawCircle(sx, sy, hf * .09f, p)
                clouds(cv, wf, hf, c(0x55FF9EB8), listOf(.12f to .35f, .7f to .3f, .4f to .5f, .85f to .55f))
                // dark horizon band
                p.shader = LinearGradient(0f, hf * .82f, 0f, hf,
                    intArrayOf(0x00000000, c(0xDD140C2A)), null, Shader.TileMode.CLAMP)
                cv.drawRect(0f, hf * .82f, wf, hf, p); p.shader = null
            }
            Sky.NIGHT -> {
                val rnd = Random(42)
                repeat(220) {
                    p.color = Color.argb(rnd.nextInt(90, 255), 255, 255, rnd.nextInt(200, 255))
                    cv.drawCircle(rnd.nextFloat() * wf, rnd.nextFloat() * hf * .75f, rnd.nextFloat() * 2.2f + .6f, p)
                }
                val mx = wf * .76f; val my = hf * .25f; val mr = hf * .085f
                glow(cv, mx, my, hf * .55f, c(0x88BFD4FF))
                p.color = c(0xFFF3F0DC); cv.drawCircle(mx, my, mr, p)
                p.color = c(0x33B9B49A)
                cv.drawCircle(mx - mr * .35f, my - mr * .2f, mr * .22f, p)
                cv.drawCircle(mx + mr * .3f, my + mr * .25f, mr * .3f, p)
                cv.drawCircle(mx + mr * .1f, my - mr * .5f, mr * .14f, p)
            }
        }

        if (rain) {
            p.color = c(0x66303A48)
            cv.drawRect(0f, 0f, wf, hf, p)
            clouds(cv, wf, hf, c(0xAA4A5360), listOf(.1f to .1f, .3f to .18f, .5f to .08f, .7f to .16f, .9f to .1f))
            val rnd = Random(7)
            p.color = c(0x66CFE3FF)
            p.strokeWidth = 2f
            repeat(260) {
                val x = rnd.nextFloat() * wf
                val y = rnd.nextFloat() * hf
                cv.drawLine(x, y, x - 10f, y + 34f, p)
            }
        }
        return bmp
    }

    private fun glow(cv: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(x, y, r, intArrayOf(color, color and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        cv.drawCircle(x, y, r, p)
    }

    private fun rays(cv: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x16FFFFFF }
        val len = w + h
        for (i in 0 until 14) {
            val deg = i * 25.7 + 8.0
            val a = Math.toRadians(deg)
            val a2 = Math.toRadians(deg + 5.0)
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x + (Math.cos(a) * len).toFloat(), y + (Math.sin(a) * len).toFloat())
                lineTo(x + (Math.cos(a2) * len).toFloat(), y + (Math.sin(a2) * len).toFloat())
                close()
            }
            cv.drawPath(path, p)
        }
    }

    private fun clouds(cv: Canvas, w: Float, h: Float, color: Int, spots: List<Pair<Float, Float>>) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            maskFilter = BlurMaskFilter(h * .02f, BlurMaskFilter.Blur.NORMAL)
        }
        for ((fx, fy) in spots) {
            val cx = w * fx; val cy = h * fy; val s = h * .06f
            cv.drawOval(RectF(cx - s * 3, cy - s * .8f, cx + s * 3, cy + s * .8f), p)
            cv.drawOval(RectF(cx - s * 1.8f, cy - s * 1.5f, cx + s * 1.2f, cy), p)
            cv.drawOval(RectF(cx - s * .2f, cy - s * 1.2f, cx + s * 2f, cy + s * .2f), p)
        }
    }
}
