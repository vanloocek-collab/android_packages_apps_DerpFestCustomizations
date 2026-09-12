/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import com.android.settings.R
import com.android.settingslib.widget.theme.R as SettingsLibR
import kotlin.math.min

/**
 * QS edit-mode silhouette: tile grid and brightness slider.
 *
 * Card keeps the wide AOSP tiles on the first two rows. Circular packs extra columns of
 * perfect circles so they sit with the same gap as the card tiles.
 */
class QsPanelStylePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scratch = RectF()
    private val brightnessIcon = drawable(R.drawable.ic_qs_style_preview_brightness)

    private var tileFill = 0
    private var accentFill = 0
    private var sliderIconColor = 0

    init {
        resolveColors()
    }

    fun setCircular(circular: Boolean, animate: Boolean) {
        val target = if (circular) 1f else 0f
        if (!animate) {
            animator?.cancel()
            progress = target
            invalidate()
            return
        }
        if (animator?.isRunning == true &&
            ((target > progress && circular) || (target < progress && !circular))
        ) {
            return
        }
        animator?.cancel()
        if (!isLaidOut) {
            post { setCircular(circular, animate = true) }
            return
        }
        if (progress == target) {
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 320L
            interpolator = EMPHASIZED
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        resolveColors()

        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val t = progress
        val inset = dp(12f)
        val contentW = width - inset * 2f
        val contentH = height - inset * 2f
        val gap = dp(7f)
        val sliderH = dp(24f)
        val tileRows = 4
        val tileH = min(dp(26f), (contentH - sliderH - gap * tileRows) / tileRows)
        val blockH = tileRows * tileH + (tileRows - 1) * gap + gap + sliderH
        val originX = inset
        val originY = inset + (contentH - blockH).coerceAtLeast(0f) / 2f

        fun x(left: Float, size: Float): Float =
            if (rtl) originX + contentW - left - size else originX + left

        val circle = tileH
        val circCols = ((contentW + gap) / (circle + gap)).toInt().coerceAtLeast(6)
        val circGap = if (circCols > 1) (contentW - circCols * circle) / (circCols - 1) else 0f
        val cardColW = (contentW - gap * 3f) / 4f
        val largeW = cardColW * 2f + gap

        fun destLeft(col: Int): Float = col * (circle + circGap)

        for (row in 0 until tileRows) {
            val top = originY + row * (tileH + gap)
            val sources = if (row < LARGE_TILE_ROWS) 2 else CARD_COLUMNS
            val sourceW = if (row < LARGE_TILE_ROWS) largeW else cardColW
            for (col in 0 until circCols) {
                val dest = destLeft(col)
                if (col < sources) {
                    val src = col * (sourceW + gap)
                    val left = lerp(src, dest, t)
                    val width = lerp(sourceW, circle, t)
                    val height = lerp(tileH, circle, t)
                    drawRound(
                        canvas,
                        x(left, width),
                        top + (tileH - height) / 2f,
                        width,
                        height,
                        height / 2f,
                        tileFill,
                    )
                } else if (t > 0.02f) {
                    val size = circle * t
                    val left = dest + (circle - size) / 2f
                    drawRound(
                        canvas,
                        x(left, size),
                        top + (tileH - size) / 2f,
                        size,
                        size,
                        size / 2f,
                        ColorUtils.setAlphaComponent(
                            tileFill,
                            (t * Color.alpha(tileFill)).toInt(),
                        ),
                    )
                }
            }
        }

        val sliderTop = originY + tileRows * tileH + tileRows * gap
        drawSlider(canvas, x(0f, contentW), sliderTop, contentW, sliderH, rtl)
    }

    private fun drawSlider(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        rtl: Boolean,
    ) {
        drawRound(canvas, left, top, width, height, height / 2f, tileFill)

        val thumbGap = dp(3f)
        val fillW = width * 0.5f - thumbGap / 2f
        canvas.save()
        if (rtl) {
            canvas.clipRect(left + width - fillW, top, left + width, top + height)
        } else {
            canvas.clipRect(left, top, left + fillW, top + height)
        }
        drawRound(canvas, left, top, width, height, height / 2f, accentFill)
        canvas.restore()

        val iconSize = dp(14f)
        val iconPad = dp(8f)
        val iconLeft = if (rtl) left + iconPad else left + width - iconPad - iconSize
        drawIcon(
            canvas,
            brightnessIcon,
            iconLeft,
            top + (height - iconSize) / 2f,
            iconSize,
            sliderIconColor,
        )
    }

    private fun drawRound(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Int,
    ) {
        if (width <= 0f || height <= 0f || Color.alpha(color) == 0) return
        fillPaint.color = color
        scratch.set(left, top, left + width, top + height)
        canvas.drawRoundRect(scratch, radius, radius, fillPaint)
    }

    private fun drawIcon(
        canvas: Canvas,
        drawable: Drawable,
        left: Float,
        top: Float,
        size: Float,
        color: Int,
    ) {
        DrawableCompat.setTint(drawable, color)
        drawable.alpha = 255
        drawable.setBounds(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
        drawable.draw(canvas)
    }

    private fun resolveColors() {
        val primary = context.getColor(SettingsLibR.color.settingslib_materialColorPrimary)
        val onSurface = context.getColor(SettingsLibR.color.settingslib_materialColorOnSurface)
        accentFill = context.getColor(SettingsLibR.color.settingslib_materialColorPrimaryContainer)
        tileFill = ColorUtils.setAlphaComponent(primary, 0x8C)
        sliderIconColor = ColorUtils.setAlphaComponent(onSurface, 0xCC)
    }

    private fun drawable(resId: Int): Drawable =
        DrawableCompat.wrap(ContextCompat.getDrawable(context, resId)!!.mutate())

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val LARGE_TILE_ROWS = 2
        private const val CARD_COLUMNS = 4
        private val EMPHASIZED = PathInterpolator(0.2f, 0f, 0f, 1f)
    }
}
