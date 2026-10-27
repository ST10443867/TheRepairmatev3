package com.example.therepairmate.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.ColorUtils
import com.example.therepairmate.R

/** Frosted "liquid glass" tile. A specular highlight follows the finger; the tile squishes and springs back. */
class GlassIconView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val dp = resources.displayMetrics.density
    private val radius = 28f * dp
    private var icon: Drawable? = null
    private var tint = Color.parseColor("#2D7BFF")
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * dp }
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var hx = 0f
    private var hy = 0f

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GlassIconView)
            icon = a.getDrawable(R.styleable.GlassIconView_glassIcon)?.mutate()
            tint = a.getColor(R.styleable.GlassIconView_glassTint, tint)
            a.recycle()
        }
        isClickable = true
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val i = edge.strokeWidth
        rect.set(i, i, w - i, h - i)
        hx = w * 0.3f; hy = h * 0.22f
        fill.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(ColorUtils.setAlphaComponent(tint, 0xB0), ColorUtils.setAlphaComponent(tint, 0x44), 0x26FFFFFF),
            null, Shader.TileMode.CLAMP
        )
        edge.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xCCFFFFFF.toInt(), 0x1AFFFFFF, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(rect, radius, radius, fill)
        shine.shader = RadialGradient(hx, hy, width * 0.65f, intArrayOf(0x99FFFFFF.toInt(), 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius, radius, shine)
        canvas.drawRoundRect(rect, radius, radius, edge)
        icon?.let {
            val s = (width * 0.42f).toInt()
            val l = (width - s) / 2
            val t = (height - s) / 2
            it.setBounds(l, t, l + s, t + s)
            it.draw(canvas)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                hx = e.x; hy = e.y
                animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start()
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                hx = e.x.coerceIn(0f, width.toFloat()); hy = e.y.coerceIn(0f, height.toFloat()); invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setInterpolator(OvershootInterpolator(3f)).setDuration(300).start()
                hx = width * 0.3f; hy = height * 0.22f; invalidate()
                if (e.actionMasked == MotionEvent.ACTION_UP && e.x in 0f..width.toFloat() && e.y in 0f..height.toFloat()) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
