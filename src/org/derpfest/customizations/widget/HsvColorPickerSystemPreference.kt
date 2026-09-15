/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.widget

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import org.derpfest.support.colorpicker.ColorPickerSystemPreference

/** [ColorPickerSystemPreference] that opens [HsvColorPickerDialog] instead of the legacy picker. */
class HsvColorPickerSystemPreference(
    context: Context,
    attrs: AttributeSet?,
) : ColorPickerSystemPreference(context, attrs) {

    init {
        setAutoSummaryEnabled(false)
    }

    override fun showDialog(state: Bundle?) {
        val titleText = title ?: ""
        HsvColorPickerDialog.show(
            context,
            displayColor,
            titleText,
            { color -> onColorChanged(color) },
            /* alphaSlider */ false,
        )
    }
}
