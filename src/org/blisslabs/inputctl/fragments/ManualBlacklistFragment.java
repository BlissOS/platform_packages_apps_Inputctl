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
import android.view.InputDevice;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.blisslabs.inputctl.R;
import org.blisslabs.inputctl.helpers.InputDeviceManagerHelper;
import org.blisslabs.inputctl.helpers.XmlHelper;

public class ManualBlacklistFragment extends PreferenceFragmentCompat {

    private InputDeviceManagerHelper mHelper;
    private PreferenceCategory mDeviceCategory;
    private PreferenceCategory mHardBlockedCategory;
    private SharedPreferences mPrefs;

    public static final String PREF_SOFT_BLACKLIST = "soft_blacklist_devices";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.manual_blacklist, rootKey);

        mHelper = new InputDeviceManagerHelper(getContext());
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        mDeviceCategory = findPreference("manual_blacklist_list");
        mHardBlockedCategory = findPreference("manual_blacklist_hard_blocked_list");

        refreshDeviceList();
    }

    private void refreshDeviceList() {
        if (mDeviceCategory == null || mHardBlockedCategory == null) return;
        
        mDeviceCategory.removeAll();
        mHardBlockedCategory.removeAll();

        String mode = mPrefs.getString("pref_blacklist_mode", "mode_soft");
        boolean isHardMode = "mode_hard".equals(mode);

        List<InputDevice> devices = mHelper.getPhysicalInputDevices();                                    // Get device name
        Set<String> hardBlacklist = XmlHelper.getBlacklistedDevices();                                          // Get hard block name
        Set<String> softBlacklist = new HashSet<>(mPrefs.getStringSet(PREF_SOFT_BLACKLIST, new HashSet<>()));   // Get soft block name

        for (InputDevice device : devices) {
            String deviceName = device.getName();
            SwitchPreference devicePref = new SwitchPreference(getPreferenceManager().getContext());
            boolean isEnabled = isHardMode ? !hardBlacklist.contains(deviceName) : !softBlacklist.contains(deviceName);
            boolean isTabletMode = mHelper.hasTabletModeSwitch(device.getId());
            boolean isPowerButton = mHelper.hasPowerButton(device.getId());

            devicePref.setKey("device_" + deviceName);
            devicePref.setPersistent(false);
            devicePref.setTitle(deviceName);
            devicePref.setChecked(isEnabled);
            devicePref.setSummary(isEnabled ? R.string.device_status_on : R.string.device_status_off);

            if (isTabletMode && isPowerButton) {
                devicePref.setSummaryOn(
                    getString(R.string.device_status_on) + "\n" +
                    getString(R.string.important_sensors_warning)
                );
            } else if (isTabletMode) {
                devicePref.setSummaryOn(
                    getString(R.string.device_status_on) + "\n" +
                    getString(R.string.tablet_mode_warning)
                );
            } else if (isPowerButton) {
                devicePref.setSummaryOn(
                    getString(R.string.device_status_on) + "\n" +
                    getString(R.string.power_button_warning)
                );
            }

            devicePref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean turnOn = (Boolean) newValue;

                // input warn
                if (!turnOn && (isTabletMode || isPowerButton)) {
                    String dialogTitle = "";
                    String dialogMsg = "";

                    if (isTabletMode && isPowerButton) {
                        dialogTitle = getString(R.string.important_sensors_warning_title);
                        dialogMsg = getString(R.string.important_sensors_warning_msg);
                    } else if (isTabletMode) {
                        dialogTitle = getString(R.string.tablet_mode_warning_title);
                        dialogMsg = getString(R.string.tablet_mode_warning_msg);
                    } else if (isPowerButton) {
                        dialogTitle = getString(R.string.power_button_warning_title);
                        dialogMsg = getString(R.string.power_button_warning_msg);
                    }

                    new AlertDialog.Builder(getContext())
                        .setTitle(dialogTitle)
                        .setMessage(dialogMsg)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            applyBlock(device, deviceName, isHardMode, hardBlacklist, softBlacklist);

                            ((SwitchPreference) preference).setChecked(false);
                            preference.setSummary(R.string.device_status_off);
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();

                    return false;
                }

                if (!turnOn) {
                    applyBlock(device, deviceName, isHardMode, hardBlacklist, softBlacklist);
                } else {
                    applyUnblock(device, deviceName, isHardMode, hardBlacklist, softBlacklist);
                }
                preference.setSummary(turnOn ? R.string.device_status_on : R.string.device_status_off);
                return true;
            });

            mDeviceCategory.addPreference(devicePref);
        }

        if (isHardMode) {
            mHardBlockedCategory.setVisible(true);
            
            for (String blockedName : hardBlacklist) {
                // If this device happens to still be active (not rebooted), 
                // skip this step to avoid repeating the list.
                boolean isAlreadyInActiveList = false;
                for (InputDevice d : devices) {
                    if (d.getName().equals(blockedName)) isAlreadyInActiveList = true;
                }
                if (isAlreadyInActiveList) continue;

                Preference blockedPref = new Preference(getPreferenceManager().getContext());
                blockedPref.setKey("hard_blocked_" + blockedName);
                blockedPref.setTitle(blockedName);
                blockedPref.setSummary(getString(R.string.hard_blocked_device_summary));
                blockedPref.setIcon(android.R.drawable.ic_delete);

                blockedPref.setOnPreferenceClickListener(preference -> {
                    new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.hard_blocked_device_restore_title))
                        .setMessage(getString(R.string.hard_blocked_device_restore_summary))
                        .setPositiveButton(getString(R.string.restore), (dialog, which) -> {
                            hardBlacklist.remove(blockedName);
                            XmlHelper.saveBlacklistedDevices(hardBlacklist);
                            mHardBlockedCategory.removePreference(preference);
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
                    return true;
                });

                mHardBlockedCategory.addPreference(blockedPref);
            }
            
            if (mHardBlockedCategory.getPreferenceCount() == 0) {
                Preference emptyPref = new Preference(getContext());
                emptyPref.setTitle(getString(R.string.empty));
                emptyPref.setSelectable(false);
                mHardBlockedCategory.addPreference(emptyPref);
            }
        } else {
            mHardBlockedCategory.setVisible(false); // Hide on Soft Block
        }
    }

    private void applyBlock(InputDevice device, String name, boolean isHardMode, Set<String> hardList, Set<String> softList) {
        if (isHardMode) {
            hardList.add(name);
            XmlHelper.saveBlacklistedDevices(hardList);
        } else {
            mHelper.disableDevice(device.getId());
            softList.add(name);
            mPrefs.edit().putStringSet(PREF_SOFT_BLACKLIST, softList).apply();
        }
    }

    private void applyUnblock(InputDevice device, String name, boolean isHardMode, Set<String> hardList, Set<String> softList) {
        if (isHardMode) {
            hardList.remove(name);
            XmlHelper.saveBlacklistedDevices(hardList);
        } else {
            mHelper.enableDevice(device.getId());
            softList.remove(name);
            mPrefs.edit().putStringSet(PREF_SOFT_BLACKLIST, softList).apply();
        }
    }
}
