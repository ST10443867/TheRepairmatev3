package com.example.therepairmate.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Views fly up one after another. Usage: listOf(card1, card2).staggerIn()  or  container.children.toList().staggerIn() */
fun List<View>.staggerIn(stepMs: Long = 60L) = forEachIndexed { i, v ->
    v.alpha = 0f; v.translationY = 48f
    v.animate().alpha(1f).translationY(0f).setStartDelay(minOf(i, 12) * stepMs).setDuration(450)
        .setInterpolator(DecelerateInterpolator(2f)).start()
}

/** Number counts up from 0.  textView.countUp(42) */
fun TextView.countUp(to: Int, durationMs: Long = 900L, format: (Int) -> String = { it.toString() }) {
    ValueAnimator.ofInt(0, to).apply {
        duration = durationMs; interpolator = DecelerateInterpolator()
        addUpdateListener { text = format(it.animatedValue as Int) }
        start()
    }
}

/** Quick left-right shake for errors.  view.shake() */
fun View.shake() {
    ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f, -16f, 14f, -10f, 6f, 0f).setDuration(420).start()
}

/** Gentle breathing pulse for "live" things. Returns the animator so you can cancel() it in onDestroy. */
fun View.pulse(): ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(
    this,
    PropertyValuesHolder.ofFloat(View.ALPHA, 1f, .45f),
    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f),
    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f)
).apply { duration = 900; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE; start() }

/** Floating levitation animation for hero cards or headers. */
fun View.levitate(): ObjectAnimator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, -10f, 0f).apply {
    duration = 3000; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE; interpolator = DecelerateInterpolator(); start()
}

/** Shimmer / glow breathing effect */
fun View.shimmerGlow(): ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(
    this,
    PropertyValuesHolder.ofFloat(View.ALPHA, 0.85f, 1f)
).apply { duration = 1200; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE; start() }

/* ---------- confetti ---------- */
class ConfettiView(context: Context) : View(context) {
    private class P(val vx: Float, val vy: Float, val vr: Float, val w: Float, val h: Float, val color: Int)
    private val parts = ArrayList<P>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cx = 0f; private var cy = 0f; private var t = 0f
    private val d = resources.displayMetrics.density
    private val colors = intArrayOf(0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(), 0xFFFFFFFF.toInt())

    fun burst(x: Float, y: Float, count: Int = 44, onDone: () -> Unit) {
        cx = x; cy = y
        repeat(count) {
            val a = Random.nextFloat() * 6.2831f; val v = (90 + Random.nextFloat() * 240) * d
            parts += P(cos(a) * v, sin(a) * v - 260f * d, (Random.nextFloat() - .5f) * 900f,
                (5 + Random.nextFloat() * 5) * d, (8 + Random.nextFloat() * 8) * d, colors[it % colors.size])
        }
        ValueAnimator.ofFloat(0f, 1.9f).apply {
            duration = 1900
            addUpdateListener { t = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() { override fun onAnimationEnd(a: android.animation.Animator) = onDone() })
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        val g = 900f * d
        for (p in parts) {
            val x = cx + p.vx * t; val y = cy + p.vy * t + .5f * g * t * t
            paint.color = p.color; paint.alpha = (255 * (1f - (t / 1.9f).coerceIn(0f, 1f).let { if (it < .65f) 0f else (it - .65f) / .35f })).toInt()
            c.save(); c.translate(x, y); c.rotate(p.vr * t); c.drawRect(-p.w / 2, -p.h / 2, p.w / 2, p.h / 2, paint); c.restore()
        }
    }
}

/** Confetti bursting from the top-centre of [anchor] (or the screen centre). */
fun Activity.confetti(anchor: View? = null) {
    val content = findViewById<ViewGroup>(android.R.id.content)
    val v = ConfettiView(this)
    content.addView(v, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    val a = IntArray(2); val b = IntArray(2)
    content.getLocationOnScreen(b)
    val x: Float; val y: Float
    if (anchor != null) { anchor.getLocationOnScreen(a); x = a[0] - b[0] + anchor.width / 2f; y = a[1] - b[1].toFloat() + anchor.height / 2f }
    else { x = content.width / 2f; y = content.height / 2f }
    v.burst(x, y) { content.removeView(v) }
}
