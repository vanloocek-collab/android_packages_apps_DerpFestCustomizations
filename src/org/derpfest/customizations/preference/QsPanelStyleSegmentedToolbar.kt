/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.android.settings.R
import com.android.settingslib.widget.theme.R as SettingsLibR

/**
 * Matches ThemePicker [TabItemAnimator] / QS edit [EditModeTabs]: each tab owns a pill whose
 * alpha and icon width animate from 0 to 1. The toolbar wraps the tabs so those pills sit
 * next to each other; the crossfade then reads as a slide.
 */
class QsPanelStyleSegmentedToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var cardTab: Tab
    private lateinit var circularTab: Tab

    private var circular = false
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var onStyleSelected: ((Boolean) -> Unit)? = null

    private val iconSize = resources.getDimensionPixelSize(R.dimen.qs_panel_style_tab_icon_size)
    private val iconMargin = resources.getDimensionPixelSize(R.dimen.qs_panel_style_tab_icon_margin)

    override fun onFinishInflate() {
        super.onFinishInflate()
        cardTab = Tab(
            findViewById(R.id.qs_style_card_tab),
            findViewById(R.id.qs_style_card_icon),
            findViewById(R.id.qs_style_card_button),
        )
        circularTab = Tab(
            findViewById(R.id.qs_style_circular_tab),
            findViewById(R.id.qs_style_circular_icon),
            findViewById(R.id.qs_style_circular_button),
        )
        findViewById<View>(R.id.qs_style_card_hit).setOnClickListener {
            onStyleSelected?.invoke(false)
        }
        findViewById<View>(R.id.qs_style_circular_hit).setOnClickListener {
            onStyleSelected?.invoke(true)
        }
        applyProgress(progress)
    }

    fun setOnStyleSelectedListener(listener: (Boolean) -> Unit) {
        onStyleSelected = listener
    }

    fun setCircular(circular: Boolean, animate: Boolean) {
        val target = if (circular) 1f else 0f
        this.circular = circular
        cardTab.setEmphasized(!circular)
        circularTab.setEmphasized(circular)
        if (!animate) {
            animator?.cancel()
            progress = target
            applyProgress(progress)
            return
        }
        if (animator?.isRunning == true &&
            ((circular && target > progress) || (!circular && target < progress))
        ) {
            return
        }
        animator?.cancel()
        if (!isLaidOut) {
            post { setCircular(circular, animate = true) }
            return
        }
        if (progress == target) {
            applyProgress(progress)
            return
        }
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = ANIMATION_DURATION_MILLIS
            interpolator = EMPHASIZED
            addUpdateListener {
                progress = it.animatedValue as Float
                applyProgress(progress)
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun applyProgress(progress: Float) {
        cardTab.apply(1f - progress)
        circularTab.apply(progress)
    }

    private inner class Tab(
        private val container: View,
        private val icon: ImageView,
        private val label: TextView,
    ) {
        init {
            container.background = container.background?.mutate()
        }

        fun setEmphasized(emphasized: Boolean) {
            label.setTextAppearance(
                if (emphasized) {
                    SettingsLibR.style.TextAppearance_SettingsLib_LabelLarge_Emphasized
                } else {
                    SettingsLibR.style.TextAppearance_SettingsLib_LabelLarge
                },
            )
        }

        fun apply(selected: Float) {
            container.background?.alpha = (selected * BACKGROUND_ALPHA_MAX).toInt()
            container.invalidate()

            val params = icon.layoutParams as MarginLayoutParams
            params.width = (selected * iconSize).toInt()
            params.marginEnd = (selected * iconMargin).toInt()
            icon.layoutParams = params
            icon.alpha = selected

            val content = ColorUtils.blendARGB(unselectedContent(), selectedContent(), selected)
            icon.imageTintList = ColorStateList.valueOf(content)
            label.setTextColor(content)
        }

        private fun selectedContent(): Int =
            context.getColor(SettingsLibR.color.settingslib_materialColorOnSecondaryContainer)

        private fun unselectedContent(): Int =
            context.getColor(SettingsLibR.color.settingslib_materialColorOnSurfaceVariant)
    }

    companion object {
        // Same as WallpaperPicker2 TabItemAnimator.
        private const val ANIMATION_DURATION_MILLIS = 200L
        private const val BACKGROUND_ALPHA_MAX = 255
        private val EMPHASIZED = PathInterpolator(0.2f, 0f, 0f, 1f)
    }
}
