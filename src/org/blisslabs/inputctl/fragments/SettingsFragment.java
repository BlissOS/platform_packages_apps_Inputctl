/*
 * Copyright (C) 2026 BlissLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.blisslabs.inputctl.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.blisslabs.inputctl.R;
import org.blisslabs.inputctl.helpers.XmlHelper;

import java.util.HashSet;
import java.util.Set;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences mPrefs;
    private String[] mModeNames;
    private String[] mModeValues;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.general_settings, rootKey);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Preference modePref = findPreference("pref_blacklist_mode_trigger");

        mModeNames = getResources().getStringArray(R.array.blacklist_mode_entries);
        mModeValues = getResources().getStringArray(R.array.blacklist_mode_values);

        if (modePref != null) {
            updateModeSummary(modePref);

            modePref.setOnPreferenceClickListener(preference -> {
                showModeSelectionDialog(modePref);
                return true;
            });
        }

        SwitchPreference safeguardPref = findPreference("pref_blacklist_safeguard");
        if (safeguardPref != null) {
            safeguardPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean turnOn = (Boolean) newValue;

                if (!turnOn) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.safeguard_disable_warning_title)
                            .setMessage(R.string.safeguard_disable_warning_msg)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                ((SwitchPreference) preference).setChecked(false);
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                    return false;
                }
                
                return true;
            });
        }
    }

    private void showResetWarning(Preference modePref, String newMode, String currentMode) {
        String title = getString(R.string.menu_settings_blacklist_mode_switch_title);
        StringBuilder message = new StringBuilder();
        
        message.append(getString(R.string.menu_settings_blacklist_mode_switch_msg) + "\n\n");

        if ("mode_hard".equals(currentMode)) {
            message.append(getString(R.string.menu_settings_blacklist_mode_switch_hardmode_extra_msg));
        }

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message.toString())
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    // Clear old data
                    if ("mode_soft".equals(currentMode)) {
                        mPrefs.edit().remove(ManualBlacklistFragment.PREF_SOFT_BLACKLIST).apply();
                    } else {
                        XmlHelper.saveBlacklistedDevices(new HashSet<>());
                    }

                    applyModeChange(modePref, newMode);
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    // Custom functions for Preferences instead of using ListPreferences because
    // ListPreferences just doesn't play nice with our style
    private void updateModeSummary(Preference modePref) {
        String currentMode = mPrefs.getString("pref_blacklist_mode", "mode_soft");

        String summary = mModeNames[0];
        for (int i = 0; i < mModeValues.length; i++) {
            if (mModeValues[i].equals(currentMode)) {
                summary = mModeNames[i];
                break;
            }
        }
        
        modePref.setSummary(summary);
    }

    private void showModeSelectionDialog(Preference modePref) {
        String currentMode = mPrefs.getString("pref_blacklist_mode", "mode_soft");

        int checkedItem = 0;
        for (int i = 0; i < mModeValues.length; i++) {
            if (mModeValues[i].equals(currentMode)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.menu_settings_blacklist_mode_title)
                .setSingleChoiceItems(mModeNames, checkedItem, (dialog, which) -> {
                    String newMode = mModeValues[which];
                    dialog.dismiss();

                    if (!newMode.equals(currentMode)) {
                        checkAndApplyNewMode(modePref, newMode, currentMode);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void checkAndApplyNewMode(Preference modePref, String newMode, String currentMode) {
        boolean hasExistingData = false;
        if ("mode_soft".equals(currentMode)) {
            Set<String> softList = mPrefs.getStringSet(ManualBlacklistFragment.PREF_SOFT_BLACKLIST, new HashSet<>());
            hasExistingData = !softList.isEmpty();
        } else {
            Set<String> hardList = XmlHelper.getBlacklistedDevices();
            hasExistingData = !hardList.isEmpty();
        }

        if (hasExistingData) {
            showResetWarning(modePref, newMode, currentMode);
        } else {
            applyModeChange(modePref, newMode);
        }
    }

    private void applyModeChange(Preference modePref, String newMode) {
        mPrefs.edit().putString("pref_blacklist_mode", newMode).apply();
        updateModeSummary(modePref);
    }
}
