/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Outline
import android.graphics.RenderEffect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import com.android.settings.R

/** Current wallpaper, blurred like the Quick Settings shade. */
class QsPanelStyleBlurBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ImageView(context, attrs, defStyleAttr) {

    private val cornerRadius = resources.getDimension(R.dimen.qs_panel_style_card_radius)

    init {
        scaleType = ScaleType.CENTER_CROP
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        val blurPx = resources.displayMetrics.density * BLUR_DP
        setRenderEffect(RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val wallpaper = try {
            WallpaperManager.getInstance(context).fastDrawable
        } catch (_: RuntimeException) {
            null
        } ?: try {
            WallpaperManager.getInstance(context).drawable
        } catch (_: RuntimeException) {
            null
        }
        setImageDrawable(wallpaper)
    }

    companion object {
        private const val BLUR_DP = 28f
    }
}
