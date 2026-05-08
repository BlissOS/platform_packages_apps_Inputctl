package org.blisslabs.inputctl.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.InputDevice;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceManager;

import org.blisslabs.inputctl.R;
import org.blisslabs.inputctl.helpers.InputDeviceManagerHelper;
import org.blisslabs.inputctl.services.TabletModeService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TabletModeFragment extends PreferenceFragmentCompat {

    private InputDeviceManagerHelper mHelper;
    private PreferenceCategory mDeviceCategory;
    private SharedPreferences mPrefs;

    public static final String PREF_AUTO_DISABLE_DEVICES = "auto_disable_devices";

    // Broadcast status to Current Mode
    private final BroadcastReceiver mTabletModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TabletModeService.ACTION_TABLET_MODE_CHANGED.equals(intent.getAction())) {
                boolean isTabletMode = intent.getBooleanExtra(TabletModeService.EXTRA_IS_TABLET_MODE, false);
                
                // Update immediately
                Preference statusPref = findPreference("pref_tablet_status");
                if (statusPref != null) {
                    statusPref.setSummary(isTabletMode ? getString(R.string.tablet_mode_folded) : getString(R.string.laptop_mode_open));
                }
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.tablet_mode, rootKey);

        mHelper = new InputDeviceManagerHelper(getContext());
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mDeviceCategory = findPreference("tablet_mode_disable_list");

        updateStatusUI();
        loadDeviceList();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        IntentFilter filter = new IntentFilter(TabletModeService.ACTION_TABLET_MODE_CHANGED);
        getContext().registerReceiver(mTabletModeReceiver, filter);
        updateStatusUI(); 
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mTabletModeReceiver);
    }

    private void updateStatusUI() {
        Preference statusPref = findPreference("pref_tablet_status");
        if (statusPref != null) {
            boolean isTablet = mHelper.isCurrentlyInTabletMode();
            statusPref.setSummary(isTablet ? getString(R.string.tablet_mode_folded) : getString(R.string.laptop_mode_open));
        }
    }

    private void loadDeviceList() {
        if (mDeviceCategory == null) return;
        mDeviceCategory.removeAll();

        List<InputDevice> devices = mHelper.getPhysicalInputDevices();
        
        // Get list
        Set<String> autoDisableSet = new HashSet<>(mPrefs.getStringSet(PREF_AUTO_DISABLE_DEVICES, new HashSet<>()));

        for (InputDevice device : devices) {
            String deviceName = device.getName();
            SwitchPreference devicePref = new SwitchPreference(getPreferenceManager().getContext());
            devicePref.setKey("auto_" + deviceName);
            devicePref.setPersistent(false);
            devicePref.setTitle(deviceName);

            boolean isTabletSwitch = mHelper.hasTabletModeSwitch(device.getId());

            if (isTabletSwitch) {
                devicePref.setEnabled(false);
                devicePref.setSummary(getString(R.string.tablet_mode_input_blocked));
            } else {
                devicePref.setChecked(autoDisableSet.contains(deviceName));
                
                devicePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isChecked = (Boolean) newValue;
                    Set<String> currentSet = new HashSet<>(mPrefs.getStringSet(PREF_AUTO_DISABLE_DEVICES, new HashSet<>()));
                    
                    if (isChecked) {
                        currentSet.add(deviceName);
                    } else {
                        currentSet.remove(deviceName);
                    }

                    mPrefs.edit().putStringSet(PREF_AUTO_DISABLE_DEVICES, currentSet).apply();
                    return true;
                });
            }

            mDeviceCategory.addPreference(devicePref);
        }
    }
}
