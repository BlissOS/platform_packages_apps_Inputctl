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

package org.blisslabs.inputctl;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.InputDevice;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import java.util.List;
import java.util.Set;

public class ManualBlacklistFragment extends PreferenceFragmentCompat {

    private InputDeviceManagerHelper mHelper;
    private PreferenceCategory mDeviceCategory;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.manual_blacklist, rootKey);

        mHelper = new InputDeviceManagerHelper(getContext());
        mDeviceCategory = findPreference("category_device_list");

        refreshDeviceList();
    }

    private void refreshDeviceList() {
        if (mDeviceCategory == null) return;
        mDeviceCategory.removeAll();

        List<InputDevice> devices = mHelper.getPhysicalInputDevices();      // Get device name
        Set<String> blacklistedNames = XmlHelper.getBlacklistedDevices();   // Get blacklisted name

        for (InputDevice device : devices) {
            String deviceName = device.getName();
            SwitchPreference devicePref = new SwitchPreference(getPreferenceManager().getContext());
            boolean isEnabled = !blacklistedNames.contains(deviceName);
            boolean isTabletMode = mHelper.hasTabletModeSwitch(device.getId());
            boolean isPowerButton = mHelper.hasPowerButton(device.getId());

            devicePref.setKey("device_" + device.getId());
            devicePref.setTitle(deviceName);
            devicePref.setChecked(isEnabled);
            devicePref.setSummary(isEnabled ? R.string.device_status_on : R.string.device_status_off);

            // Tablet Mode input warn
            if (isTabletMode) {
                devicePref.setSummaryOn(
                    getString(R.string.device_status_on) + "\n" +
                    getString(R.string.tablet_mode_warning)
                );
            }

            // Power Button input warn
            if (isPowerButton) {
                devicePref.setSummaryOn(
                    getString(R.string.device_status_on) + "\n" +
                    getString(R.string.power_button_warning)
                );
            }

            devicePref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean turnOn = (Boolean) newValue;

                // Tablet Mode input warn
                if (!turnOn && isTabletMode) {
                    new AlertDialog.Builder(getContext())
                        .setTitle(R.string.tablet_mode_warning_title)
                        .setMessage(R.string.tablet_mode_warning_msg)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            mHelper.disableDevice(device.getId());
                            blacklistedNames.add(deviceName);
                            XmlHelper.saveBlacklistedDevices(blacklistedNames);

                            ((SwitchPreference) preference).setChecked(false);
                            preference.setSummary(R.string.device_status_off);
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();

                    return false;
                }

                // Power Button input warn
                if (!turnOn && isPowerButton) {
                    new AlertDialog.Builder(getContext())
                        .setTitle(R.string.power_button_warning_title)
                        .setMessage(R.string.power_button_warning_msg)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            mHelper.disableDevice(device.getId());
                            blacklistedNames.add(deviceName);
                            XmlHelper.saveBlacklistedDevices(blacklistedNames);

                            ((SwitchPreference) preference).setChecked(false);
                            preference.setSummary(R.string.device_status_off);
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();

                    return false;
                }

                // Reload
                if (turnOn) {
                    mHelper.enableDevice(device.getId());
                    blacklistedNames.remove(deviceName);
                } else {
                    mHelper.disableDevice(device.getId());
                    blacklistedNames.add(deviceName);
                }

                // Save to XML
                XmlHelper.saveBlacklistedDevices(blacklistedNames);
                preference.setSummary(turnOn ? R.string.device_status_on : R.string.device_status_off);
                return true;
            });

            mDeviceCategory.addPreference(devicePref);
        }
    }
}
