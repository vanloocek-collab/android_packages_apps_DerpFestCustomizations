/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.derpfest.customizations.utils.DeviceUtils;

public class PulseSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_PULSE_RENDERER = "pulse_renderer";
    private static final String KEY_PULSE_ROUNDED_BARS = "pulse_rounded_bars";
    private static final String KEY_PULSE_COLOR = "pulse_color";
    private static final String KEY_PULSE_BAR_COUNT = "pulse_bar_count";
    private static final String KEY_PULSE_HAPTICS = "pulse_haptics_enabled";

    private ListPreference mPulseRenderer;
    private Preference mPulseRoundedBars;
    private Preference mPulseColor;
    private Preference mPulseBarCount;
    private Preference mPulseHaptics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pulse_settings);

        mPulseRenderer = findPreference(KEY_PULSE_RENDERER);
        mPulseRoundedBars = findPreference(KEY_PULSE_ROUNDED_BARS);
        mPulseColor = findPreference(KEY_PULSE_COLOR);
        mPulseBarCount = findPreference(KEY_PULSE_BAR_COUNT);
        mPulseHaptics = findPreference(KEY_PULSE_HAPTICS);

        if (mPulseHaptics != null && !DeviceUtils.hasVibrator(getContext())) {
            mPulseHaptics.setVisible(false);
        }

        if (mPulseRenderer != null) {
            mPulseRenderer.setOnPreferenceChangeListener(this);
            updatePreferenceVisibility(mPulseRenderer.getValue());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPulseRenderer) {
            updatePreferenceVisibility((String) newValue);
        }
        return true;
    }

    /**
     * Rounding is only applied by SolidLine and Waveform in SystemUI.
     * Retro VU uses hardcoded segment colors and ignores pulse_color.
     * Particle does not draw bars, so bar count has no visible effect.
     */
    private void updatePreferenceVisibility(String rendererValue) {
        if (rendererValue == null) {
            return;
        }

        boolean supportsRounding = "solid".equals(rendererValue)
                || "waveform".equals(rendererValue);
        boolean supportsColoring = !"retro".equals(rendererValue);
        boolean supportsBarCount = !"particle".equals(rendererValue);

        if (mPulseRoundedBars != null) {
            mPulseRoundedBars.setVisible(supportsRounding);
        }
        if (mPulseColor != null) {
            mPulseColor.setVisible(supportsColoring);
        }
        if (mPulseBarCount != null) {
            mPulseBarCount.setVisible(supportsBarCount);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DERPFEST;
    }
}
