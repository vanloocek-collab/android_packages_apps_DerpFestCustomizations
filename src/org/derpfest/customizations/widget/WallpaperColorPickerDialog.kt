/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.widget

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.ImageColorPicker
import com.github.skydoves.colorpicker.compose.PaletteContentScale
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.function.Consumer

/** Wallpaper-backed picker: the selected color is the wallpaper pixel, not a brightness-adjusted one. */
object WallpaperColorPickerDialog {

    @JvmStatic
    fun show(
        context: Context,
        wallpaper: Drawable,
        initialColor: Int,
        title: CharSequence,
        onColorPicked: Consumer<Int>,
    ) {
        showComposeColorPickerDialog(context, initialColor, title, onColorPicked) { onColorChanged ->
            val controller = rememberColorPickerController()
            val palette = remember(wallpaper) { wallpaper.toImageBitmap() }

            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                ImageColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    controller = controller,
                    paletteImageBitmap = palette,
                    paletteContentScale = PaletteContentScale.FIT,
                    onColorChanged = { envelope ->
                        if (envelope.fromUser) {
                            onColorChanged(envelope.color.toArgb())
                        }
                    },
                )
                ColorPreview(controller)
            }
        }
    }
}

/**
 * HSV wheel plus brightness (and optional alpha). Use this for prefs that choose a color
 * rather than sampling a wallpaper.
 */
object HsvColorPickerDialog {

    @JvmStatic
    @JvmOverloads
    fun show(
        context: Context,
        initialColor: Int,
        title: CharSequence,
        onColorPicked: Consumer<Int>,
        alphaSlider: Boolean = false,
    ) {
        showComposeColorPickerDialog(context, initialColor, title, onColorPicked) { onColorChanged ->
            val controller = rememberColorPickerController()
            val startColor = Color(initialColor)

            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    controller = controller,
                    initialColor = startColor,
                    onColorChanged = { envelope ->
                        if (envelope.fromUser) {
                            onColorChanged(envelope.color.toArgb())
                        }
                    },
                )
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(28.dp),
                    controller = controller,
                    initialColor = startColor,
                )
                if (alphaSlider) {
                    AlphaSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(28.dp),
                        controller = controller,
                        initialColor = startColor,
                    )
                }
                ColorPreview(controller)
            }
        }
    }
}

@Composable
private fun ColorPreview(controller: ColorPickerController) {
    AlphaTile(
        modifier = Modifier
            .padding(top = 16.dp)
            .size(48.dp)
            .clip(CircleShape),
        controller = controller,
    )
}

private fun showComposeColorPickerDialog(
    context: Context,
    initialColor: Int,
    title: CharSequence,
    onColorPicked: Consumer<Int>,
    content: @Composable (onColorChanged: (Int) -> Unit) -> Unit,
) {
    val selectedColor = intArrayOf(initialColor)
    val composeView = ComposeView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        context.findLifecycleOwner()?.let { owner ->
            setViewTreeLifecycleOwner(owner)
            if (owner is SavedStateRegistryOwner) {
                setViewTreeSavedStateRegistryOwner(owner)
            }
        }
        setContent {
            content { selectedColor[0] = it }
        }
    }

    MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setView(composeView)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onColorPicked.accept(selectedColor[0])
        }
        .show()
}

private fun Drawable.toImageBitmap() = when {
    this is BitmapDrawable && bitmap != null -> bitmap.asImageBitmap()
    else -> {
        val width = intrinsicWidth.coerceAtLeast(1)
        val height = intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap.asImageBitmap()
    }
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}
