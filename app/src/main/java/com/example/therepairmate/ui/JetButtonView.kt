package com.example.therepairmate.ui

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.example.therepairmate.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Capsule "Send" button. Tap: runway slides up, plane taxis, accelerates (afterburner), rotates and
 * climbs away, then the capsule turns green "Sent ✓" and resets.
 *
 *   button.validate = { form is valid }                 // false -> button shakes
 *   button.onSend   = { doTheWork(); true }             // suspend; return true = success
 *   button.onResult = { ok -> ... }                     // called the moment the result is known
 */
class JetButtonView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private enum class S { IDLE, RUN, SENT, FAIL }

    var validate: (() -> Boolean)? = null
    var onSend: (suspend () -> Boolean)? = null
    var onResult: ((Boolean) -> Unit)? = null

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private var state = S.IDLE
    private var idleLabel = "Send Message"
    private var text = idleLabel
    var label: String
        get() = idleLabel
        set(v) { idleLabel = v; if (state == S.IDLE) text = v; contentDescription = v; invalidate() }

    // geometry
    private val btnH = dp(58f)
    private val gap = dp(12f)
    private val runH = dp(180f)
    private var expand = 0f

    // colours
    private val dark = 0xFF0B0B0F.toInt()
    private val emerald = 0xFF10B981.toInt()
    private var fillColor = dark
    private var strokeColor = Color.WHITE

    // timeline
    private val flightMs = 3800L
    private var flightStart = 0L
    private var sentAt = 0L
    private val scope = MainScope()
    private var ticker: ValueAnimator? = null

    // paints
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = dp(16f); letterSpacing = 0.03f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val rect = RectF()
    private val skyPaint = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, runH * .55f, 0xFF04050D.toInt(), 0xFF111A3D.toInt(), Shader.TileMode.CLAMP)
    }
    private val tarmacPaint = Paint().apply {
        shader = LinearGradient(0f, runH * .55f, 0f, runH, 0xFF2A2E3D.toInt(), 0xFF1A1D28.toInt(), Shader.TileMode.CLAMP)
    }
    private val plumePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, -dp(46f), 0f,
            intArrayOf(Color.WHITE, 0xFFFFD27A.toInt(), 0xFFFF7A2A.toInt(), 0x00FF7A2A),
            floatArrayOf(0f, .3f, .65f, 1f), Shader.TileMode.CLAMP
        )
    }
    private val shimmerPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 1f, 0f,
            intArrayOf(0x00FFFFFF, 0x99FFFFFF.toInt(), 0x00FFFFFF), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP
        )
    }

    // aircraft, drawn in a 72 x 30 box
    private val fuselage = Path().apply {
        moveTo(4f, 15f); rCubicTo(0f, -4f, 5f, -6f, 12f, -6f); rLineTo(36f, 0f)
        rCubicTo(10f, 0f, 17f, 3f, 19f, 6f); rCubicTo(-2f, 3f, -9f, 6f, -19f, 6f)
        lineTo(16f, 21f); cubicTo(9f, 21f, 4f, 19f, 4f, 15f); close()
    }
    private val tail = Path().apply { moveTo(6f, 13f); lineTo(2f, 2f); lineTo(13f, 2f); lineTo(21f, 11f); close() }
    private val wing = Path().apply { moveTo(30f, 17f); lineTo(48f, 17f); lineTo(37f, 27f); lineTo(26f, 27f); close() }
    private val cockpit = Path().apply { moveTo(56f, 11.6f); rQuadTo(6f, .6f, 9f, 3.2f); lineTo(54f, 14.8f); close() }

    private val stars = floatArrayOf(.12f, .22f, .30f, .60f, .52f, .18f, .71f, .48f, .88f, .26f, .63f, .78f, .40f, .38f)
    private val plumeKeys = floatArrayOf(0f, .26f, .36f, .62f, .86f, 1f)
    private val plumeAlpha = floatArrayOf(0f, 0f, .75f, 1f, 1f, 0f)
    private val plumeScale = floatArrayOf(.1f, .1f, .8f, 1.5f, 2f, 2.2f)
    private val dec = DecelerateInterpolator()
    private val acc = AccelerateInterpolator(1.8f)
    private val acc2 = AccelerateInterpolator(1.4f)

    // current plane pose
    private var pTx = 0f; private var pTy = 0f; private var pRot = 0f; private var pSc = 1f

    init {
        isClickable = true
        isFocusable = true
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.JetButtonView)
            a.getString(R.styleable.JetButtonView_jetLabel)?.let { s -> label = s }
            a.recycle()
        }
        contentDescription = label
    }

    /* ---------- lifecycle ---------- */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ticker = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { invalidate() }; start()
        }
    }

    override fun onDetachedFromWindow() {
        ticker?.cancel(); ticker = null
        scope.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = if (MeasureSpec.getMode(wSpec) == MeasureSpec.UNSPECIFIED) dp(320f).toInt() else MeasureSpec.getSize(wSpec)
        setMeasuredDimension(w, (btnH + expand * (gap + runH)).toInt())
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Button"
    }

    /* ---------- input ---------- */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (state == S.IDLE) when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> animate().scaleX(.97f).scaleY(.97f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        }
        return super.onTouchEvent(e)
    }

    override fun performClick(): Boolean {
        super.performClick()
        fire()
        return true
    }

    private fun shake() {
        ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f, -dp(8f), dp(7f), -dp(5f), dp(3f), 0f).setDuration(450).start()
    }

    private fun fire() {
        if (state != S.IDLE) return
        if (validate?.invoke() == false) { shake(); return }
        state = S.RUN
        scope.launch {
            val sending = async { runCatching { onSend?.invoke() ?: true }.getOrDefault(false) }
            val reduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            if (!reduced) {
                text = "Cleared for takeoff"
                animateExpand(1f)
                delay(650)
                flightStart = SystemClock.uptimeMillis()
                delay(flightMs)
            }
            val ok = sending.await()
            animateExpand(0f)
            onResult?.invoke(ok)
            if (ok) {
                state = S.SENT; text = "Sent ✓"; sentAt = SystemClock.uptimeMillis()
                animateColors(emerald, 0xFF34D399.toInt()); delay(2400)
            } else {
                state = S.FAIL; text = "Couldn't send"
                animateColors(0xFF7F1D1D.toInt(), 0xFFF87171.toInt()); delay(2000)
            }
            animateColors(dark, Color.WHITE)
            text = idleLabel; flightStart = 0L; state = S.IDLE
        }
    }

    private fun animateExpand(to: Float) {
        val from = expand
        ValueAnimator.ofFloat(from, to).apply {
            duration = 600; interpolator = DecelerateInterpolator()
            addUpdateListener { expand = it.animatedValue as Float; requestLayout(); invalidate() }
            start()
        }
    }

    private fun animateColors(toFill: Int, toStroke: Int) {
        val f0 = fillColor; val s0 = strokeColor; val ev = ArgbEvaluator()
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            addUpdateListener {
                val f = it.animatedFraction
                fillColor = ev.evaluate(f, f0, toFill) as Int
                strokeColor = ev.evaluate(f, s0, toStroke) as Int
                invalidate()
            }
            start()
        }
    }

    /* ---------- drawing ---------- */
    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val now = SystemClock.uptimeMillis()
        drawCapsule(c, w, now)
        if (expand > 0.001f) drawRunway(c, w, now)
    }

    private fun fillPath(c: Canvas, path: Path, color: Int) { p.style = Paint.Style.FILL; p.color = color; c.drawPath(path, p) }

    private fun drawPlane(c: Canvas, gearAlpha: Float) {
        p.style = Paint.Style.FILL
        if (gearAlpha > 0f) {
            p.color = 0xFFB9C0D8.toInt(); p.alpha = (255 * gearAlpha).toInt()
            c.drawRect(26f, 20f, 28f, 26f, p); c.drawRect(50f, 20f, 52f, 26f, p)
            p.color = 0xFFE8EAF4.toInt(); p.alpha = (255 * gearAlpha).toInt()
            c.drawCircle(27f, 27f, 2.4f, p); c.drawCircle(51f, 27f, 2.4f, p)
        }
        fillPath(c, tail, 0xFFE6ECFF.toInt()); fillPath(c, wing, 0xFFCFD8FB.toInt())
        fillPath(c, fuselage, Color.WHITE); fillPath(c, cockpit, 0xFF2D7BFF.toInt())
        p.color = 0xFF9BB0FF.toInt()
        for (x in floatArrayOf(22f, 28f, 34f, 40f)) c.drawCircle(x, 14.5f, 1.3f, p)
    }

    private fun drawCapsule(c: Canvas, w: Float, now: Long) {
        val inset = dp(1f); val r = btnH / 2f
        rect.set(inset, inset, w - inset, btnH - inset)
        p.style = Paint.Style.FILL; p.color = fillColor; c.drawRoundRect(rect, r, r, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = strokeColor; c.drawRoundRect(rect, r, r, p)
        p.style = Paint.Style.FILL

        val showIcon = state != S.SENT
        val iconW = dp(50f); val iconH = iconW * 30f / 72f; val sp = dp(14f)
        val tw = textP.measureText(text)
        val x = (w - (tw + if (showIcon) sp + iconW else 0f)) / 2f
        val fm = textP.fontMetrics
        c.drawText(text, x, btnH / 2f - (fm.ascent + fm.descent) / 2f, textP)
        if (showIcon) {
            val pulse = (sin(now / 350.0).toFloat() + 1f) / 2f          // turbine pulse
            c.save()
            c.translate(x + tw + sp + pulse * dp(3f), btnH / 2f - iconH / 2f)
            val k = iconW / 72f; c.scale(k, k)
            drawPlane(c, 0f)
            p.color = 0xFFFFB347.toInt(); p.alpha = (110 + 145 * pulse).toInt()
            c.drawCircle(38f, 20.5f, 2f + 2.4f * pulse, p)
            c.restore()
        }
        if (state == S.SENT) {                                              // light sweep
            val f = ((now - sentAt - 200) / 1100f).coerceIn(0f, 1f)
            if (f > 0f && f < 1f) {
                val band = w * .4f
                c.save()
                val clip = Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) }
                c.clipPath(clip)
                c.translate(-band + f * (w + band), 0f)
                c.scale(band, 1f)
                c.drawRect(0f, 0f, 1f, btnH, shimmerPaint)
                c.restore()
            }
        }
    }

    private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

    private fun table(keys: FloatArray, vals: FloatArray, t: Float): Float {
        for (i in 1 until keys.size) if (t <= keys[i]) return lerp(vals[i - 1], vals[i], (t - keys[i - 1]) / (keys[i] - keys[i - 1]))
        return vals.last()
    }

    private fun pose(t: Float, w: Float) {
        pTy = 0f; pRot = 0f; pSc = 1f
        when {
            t < .28f -> pTx = lerp(0f, w * .14f, dec.getInterpolation(t / .28f))                       // slow taxi
            t < .62f -> pTx = lerp(w * .14f, w * .56f, acc.getInterpolation((t - .28f) / .34f))        // rapid acceleration
            t < .72f -> {                                                                              // nose rotation
                val f = dec.getInterpolation((t - .62f) / .10f)
                pTx = lerp(w * .56f, w * .66f, f); pTy = lerp(0f, -dp(4f), f); pRot = lerp(0f, -14f, f)
            }
            else -> {                                                                                  // climb out
                val f = acc2.getInterpolation((t - .72f) / .28f)
                pTx = lerp(w * .66f, w + dp(90f), f); pTy = lerp(-dp(4f), -runH * .85f, f)
                pRot = lerp(-14f, -26f, f); pSc = lerp(1f, .55f, f)
            }
        }
    }

    private fun drawRunway(c: Canvas, w: Float, now: Long) {
        val top = btnH + gap
        rect.set(0f, top, w, top + runH * expand)
        c.save()
        c.clipPath(Path().apply { addRoundRect(rect, dp(24f), dp(24f), Path.Direction.CW) })
        c.translate(0f, top)

        val ty0 = runH * .55f; val tarH = runH * .45f
        c.drawRect(0f, 0f, w, ty0, skyPaint)
        p.style = Paint.Style.FILL; p.color = Color.WHITE
        var i = 0
        while (i < stars.size) { c.drawCircle(stars[i] * w, stars[i + 1] * ty0, dp(if (i % 4 == 0) 1.3f else .8f), p); i += 2 }
        c.drawRect(0f, ty0, w, runH, tarmacPaint)
        p.color = 0x1FFFFFFF; c.drawRect(0f, ty0, w, ty0 + dp(2f), p)

        // threshold piano markings
        p.color = 0xEBECEEF6.toInt()
        var y = ty0 + tarH * .15f; val yEnd = runH - tarH * .15f
        while (y < yEnd) { c.drawRect(dp(10f), y, dp(54f), minOf(y + dp(5f), yEnd), p); y += dp(10f) }

        // flight progress
        val t = if (flightStart == 0L) 0f else ((now - flightStart) / flightMs.toFloat()).coerceIn(0f, 1f)

        // streaking centre line
        val period = dp(52f)
        val f = ((t - .26f) / .5f).coerceIn(0f, 1f)
        val shift = (acc.getInterpolation(f) * w * 2.4f) % period
        p.color = 0xE6FFFFFF.toInt()
        val cy = ty0 + tarH / 2f
        var x = dp(68f) - shift
        while (x < w) {
            val l = maxOf(x, dp(68f)); val r = minOf(x + dp(26f), w)
            if (r > l) c.drawRect(l, cy - dp(1.5f), r, cy + dp(1.5f), p)
            x += period
        }

        // sequential edge lights
        val n = 14
        for (k in 0 until n) {
            val cyc = (((now - k * 70L) % 1000L) + 1000L) % 1000L / 1000f
            val v = if (cyc < .35f) cyc / .35f else 1f - (cyc - .35f) / .65f
            val lx = (k + .5f) / n * w
            for (ly in floatArrayOf(ty0 + dp(7f), runH - dp(7f))) {
                if (v > .5f) { p.color = 0xFFFFD36B.toInt(); p.alpha = (70 * v).toInt(); c.drawCircle(lx, ly, dp(7f), p) }
                p.color = 0xFFFFD36B.toInt(); p.alpha = (255 * (.25f + .75f * v)).toInt(); c.drawCircle(lx, ly, dp(3f), p)
            }
        }
        p.alpha = 255

        // the aircraft
        pose(t, w)
        val cx0 = w * .06f + dp(32f)
        val cy0 = runH - (runH * .225f - dp(6f)) - dp(13.5f)
        c.save()
        c.translate(cx0 + pTx, cy0 + pTy); c.rotate(pRot); c.scale(pSc, pSc)
        plumePaint.alpha = (255 * table(plumeKeys, plumeAlpha, t)).toInt()                              // afterburner
        c.save(); c.translate(-dp(27f), -dp(2.2f)); c.scale(table(plumeKeys, plumeScale, t), 1f)
        c.drawRect(-dp(46f), -dp(4.5f), 0f, dp(4.5f), plumePaint); c.restore()
        c.translate(-dp(32f), -dp(13.5f))
        val k = dp(64f) / 72f; c.scale(k, k)
        val gear = if (t < .72f) 1f else if (t < .8f) 1f - (t - .72f) / .08f else 0f                    // gear up
        drawPlane(c, gear)
        c.restore()
        c.restore()

        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1f); p.color = 0x29FFFFFF                       // runway border
        c.drawRoundRect(rect, dp(24f), dp(24f), p)
        p.style = Paint.Style.FILL
    }
}
