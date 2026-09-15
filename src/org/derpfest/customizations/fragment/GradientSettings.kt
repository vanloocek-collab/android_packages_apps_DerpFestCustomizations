/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.database.ContentObserver
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import org.derpfest.support.colorpicker.ColorPickerSystemPreference

class GradientSettings : SettingsPreferenceFragment(), OnPreferenceChangeListener {

    private var gradientColorsCategory: PreferenceCategory? = null
    private var startColorPref: ColorPickerSystemPreference? = null
    private var endColorPref: ColorPickerSystemPreference? = null
    private var chipGradientPreference: Preference? = null
    private var dualShadeObserver: ContentObserver? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.gradient_settings)
        gradientColorsCategory = findPreference("gradient_colors_category")
        startColorPref = findPreference("gradient_start_color")
        endColorPref = findPreference("gradient_end_color")
        chipGradientPreference = findPreference("qs_chip_gradient_enabled")
        findPreference<Preference>("qs_tile_gradient_enabled")?.setOnPreferenceChangeListener(this)
        findPreference<Preference>("qs_brightness_gradient_enabled")?.setOnPreferenceChangeListener(this)
        findPreference<Preference>("qs_volume_gradient_enabled")?.setOnPreferenceChangeListener(this)
        chipGradientPreference?.setOnPreferenceChangeListener(this)
        updateChipGradientAvailability()
        updateColorPickersAvailability()
    }

    override fun onStart() {
        super.onStart()
        dualShadeObserver =
            QsShadePanels.registerDualShadeObserver(requireContext().contentResolver) {
                updateChipGradientAvailability()
                updateColorPickersAvailability()
            }
        updateChipGradientAvailability()
        updateColorPickersAvailability()
    }

    override fun onStop() {
        dualShadeObserver?.let { requireContext().contentResolver.unregisterContentObserver(it) }
        dualShadeObserver = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        updateChipGradientAvailability()
        updateColorPickersAvailability()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        updateColorPickersAvailability(changedKey = preference.key, newValue = newValue)
        return true
    }

    private fun isChipGradientAvailable(): Boolean =
        QsShadePanels.isDualShadeEnabled(requireContext())

    private fun updateChipGradientAvailability() {
        val available = isChipGradientAvailable()
        chipGradientPreference?.isEnabled = available
        chipGradientPreference?.setSummary(
            if (available) {
                R.string.qs_chip_gradient_enabled_summary
            } else {
                R.string.qs_chip_gradient_requires_separate_qs
            },
        )
    }

    /**
     * Updates gradient start/end availability. When called from onPreferenceChange, the changed
     * preference is not yet written to Settings, so we pass [changedKey] and [newValue] to use
     * the new value for that toggle and avoid wrong availability (e.g. needing two toggles to
     * enable, or category staying enabled when all are turned off).
     */
    private fun updateColorPickersAvailability(changedKey: String? = null, newValue: Any? = null) {
        val tile = toggleEnabled("qs_tile_gradient_enabled", changedKey, newValue)
        val brightness = toggleEnabled("qs_brightness_gradient_enabled", changedKey, newValue)
        val volume = toggleEnabled("qs_volume_gradient_enabled", changedKey, newValue)
        val chip = isChipGradientAvailable() &&
            toggleEnabled("qs_chip_gradient_enabled", changedKey, newValue)
        val colorsEnabled = tile || brightness || volume || chip
        gradientColorsCategory?.isEnabled = colorsEnabled
        val colorSummary = if (colorsEnabled) {
            null
        } else {
            getString(R.string.gradient_colors_requires_toggle)
        }
        startColorPref?.summary = colorSummary
            ?: getString(R.string.gradient_start_color_summary)
        endColorPref?.summary = colorSummary
            ?: getString(R.string.gradient_end_color_summary)
    }

    private fun toggleEnabled(key: String, changedKey: String?, newValue: Any?): Boolean {
        if (changedKey == key) {
            return newValue as? Boolean
                ?: (Settings.System.getInt(requireContext().contentResolver, key, 1) != 0)
        }
        return Settings.System.getInt(requireContext().contentResolver, key, 1) != 0
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "GradientSettings"
    }
}
