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
import android.os.Bundle;
import android.view.InputDevice;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import org.blisslabs.inputctl.R;
import org.blisslabs.inputctl.helpers.InputDeviceManagerHelper;
import org.blisslabs.inputctl.helpers.KbdConfigHelper;
import org.blisslabs.inputctl.helpers.KbdConfigHelper.KbdDeviceConfig;
import org.blisslabs.inputctl.helpers.KbdConfigHelper.KbdActionConfig;

import java.util.ArrayList;
import java.util.List;

public class KbdSensorConfigFragment extends PreferenceFragmentCompat {

    private InputDeviceManagerHelper mInputHelper;
    private PreferenceCategory mAddDeviceCategory;
    private PreferenceCategory mDeviceListCategory;
    private List<KbdDeviceConfig> mConfigs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.kbd_sensor_config, rootKey);

        mInputHelper = new InputDeviceManagerHelper(getContext());
        mAddDeviceCategory = findPreference("kbd_sensor_add_device");
        mDeviceListCategory = findPreference("kbd_sensor_device_list");

        mConfigs = KbdConfigHelper.loadConfig();
        refreshDeviceList();
    }

    private void refreshDeviceList() {
        if (mAddDeviceCategory == null || mDeviceListCategory == null) return;

        mAddDeviceCategory.removeAll();
        mDeviceListCategory.removeAll();

        // --- Build the "Add Device" picker ---
        List<InputDevice> physicalDevices = mInputHelper.getPhysicalInputDevices();
        List<String> configuredNames = new ArrayList<>();
        for (KbdDeviceConfig cfg : mConfigs) {
            configuredNames.add(cfg.name);
        }

        for (InputDevice device : physicalDevices) {
            String deviceName = device.getName();
            Preference addPref = new Preference(getPreferenceManager().getContext());
            addPref.setKey("add_" + deviceName);
            addPref.setTitle(deviceName);
            addPref.setIcon(R.drawable.ic_add);

            if (configuredNames.contains(deviceName)) {
                addPref.setEnabled(false);
                addPref.setSummary(R.string.kbd_sensor_config_already_added);
            } else {
                addPref.setSummary(R.string.kbd_sensor_config_tap_to_add);
                addPref.setOnPreferenceClickListener(preference -> {
                    KbdDeviceConfig newConfig = new KbdDeviceConfig();
                    newConfig.name = deviceName;
                    mConfigs.add(newConfig);
                    KbdConfigHelper.saveConfig(mConfigs);
                    refreshDeviceList();
                    return true;
                });
            }
            mAddDeviceCategory.addPreference(addPref);
        }

        // --- Build the configured device cards ---
        for (KbdDeviceConfig config : mConfigs) {
            // Device header preference (with remove action)
            Preference headerPref = new Preference(getPreferenceManager().getContext());
            headerPref.setKey("header_" + config.name);
            headerPref.setTitle(config.name);
            headerPref.setIcon(R.drawable.ic_close);
            headerPref.setSummary(R.string.kbd_sensor_config_tap_to_remove);
            headerPref.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getContext())
                    .setTitle(R.string.kbd_sensor_config_remove_title)
                    .setMessage(getString(R.string.kbd_sensor_config_remove_msg, config.name))
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        mConfigs.remove(config);
                        KbdConfigHelper.saveConfig(mConfigs);
                        refreshDeviceList();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
                return true;
            });
            mDeviceListCategory.addPreference(headerPref);

            // Rotation rows
            addRotationPreference(config, "rot0", R.string.kbd_sensor_config_rot0, config.rot0);
            addRotationPreference(config, "rot90", R.string.kbd_sensor_config_rot90, config.rot90);
            addRotationPreference(config, "rot180", R.string.kbd_sensor_config_rot180, config.rot180);
            addRotationPreference(config, "rot270", R.string.kbd_sensor_config_rot270, config.rot270);
        }

        if (mConfigs.isEmpty()) {
            Preference emptyPref = new Preference(getContext());
            emptyPref.setTitle(R.string.empty);
            emptyPref.setSelectable(false);
            mDeviceListCategory.addPreference(emptyPref);
        }
    }

    private void addRotationPreference(KbdDeviceConfig config, String rotId, int titleRes, KbdActionConfig action) {
        Preference rotPref = new Preference(getPreferenceManager().getContext());
        rotPref.setKey(rotId + "_" + config.name);
        rotPref.setTitle(titleRes);
        rotPref.setSummary(formatAction(action));

        rotPref.setOnPreferenceClickListener(preference -> {
            KeyCaptureDialogFragment dialog = new KeyCaptureDialogFragment();
            dialog.setListener(new KeyCaptureDialogFragment.KeyCaptureListener() {
                @Override
                public void onKeyCaptured(KbdActionConfig newAction) {
                    action.key = newAction.key;
                    action.mod1 = newAction.mod1;
                    action.mod2 = newAction.mod2;
                    KbdConfigHelper.saveConfig(mConfigs);
                    rotPref.setSummary(formatAction(action));
                }

                @Override
                public void onKeyCleared() {
                    action.key = -1;
                    action.mod1 = -1;
                    action.mod2 = -1;
                    KbdConfigHelper.saveConfig(mConfigs);
                    rotPref.setSummary(formatAction(action));
                }
            });
            dialog.show(getParentFragmentManager(), "KeyCaptureDialog");
            return true;
        });

        mDeviceListCategory.addPreference(rotPref);
    }

    private String formatAction(KbdActionConfig action) {
        if (action.key == -1) {
            return getString(R.string.kbd_sensor_config_not_mapped);
        }

        StringBuilder sb = new StringBuilder();
        if (action.mod1 != -1) sb.append(scanCodeToLabel(action.mod1)).append(" + ");
        if (action.mod2 != -1) sb.append(scanCodeToLabel(action.mod2)).append(" + ");
        sb.append(scanCodeToLabel(action.key));
        return sb.toString();
    }

    /**
     * Convert a Linux scancode to a human-readable label.
     * Covers common modifier and alphanumeric keys.
     */
    private static String scanCodeToLabel(int scanCode) {
        switch (scanCode) {
            case 1: return "Esc";
            case 2: return "1";
            case 3: return "2";
            case 4: return "3";
            case 5: return "4";
            case 6: return "5";
            case 7: return "6";
            case 8: return "7";
            case 9: return "8";
            case 10: return "9";
            case 11: return "0";
            case 14: return "Backspace";
            case 15: return "Tab";
            case 16: return "Q";
            case 17: return "W";
            case 18: return "E";
            case 19: return "R";
            case 20: return "T";
            case 21: return "Y";
            case 22: return "U";
            case 23: return "I";
            case 24: return "O";
            case 25: return "P";
            case 28: return "Enter";
            case 29: return "LCtrl";
            case 30: return "A";
            case 31: return "S";
            case 32: return "D";
            case 33: return "F";
            case 34: return "G";
            case 35: return "H";
            case 36: return "J";
            case 37: return "K";
            case 38: return "L";
            case 42: return "LShift";
            case 44: return "Z";
            case 45: return "X";
            case 46: return "C";
            case 47: return "V";
            case 48: return "B";
            case 49: return "N";
            case 50: return "M";
            case 54: return "RShift";
            case 56: return "LAlt";
            case 57: return "Space";
            case 58: return "CapsLock";
            case 59: return "F1";
            case 60: return "F2";
            case 61: return "F3";
            case 62: return "F4";
            case 63: return "F5";
            case 64: return "F6";
            case 65: return "F7";
            case 66: return "F8";
            case 67: return "F9";
            case 68: return "F10";
            case 87: return "F11";
            case 88: return "F12";
            case 97: return "RCtrl";
            case 100: return "RAlt";
            case 103: return "Up";
            case 105: return "Left";
            case 106: return "Right";
            case 108: return "Down";
            case 125: return "Super";
            default: return "SC" + scanCode;
        }
    }
}
