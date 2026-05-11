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

package org.blisslabs.inputctl.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.hardware.input.InputManager;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;

import androidx.preference.PreferenceManager;

import org.blisslabs.inputctl.R;
import org.blisslabs.inputctl.fragments.TabletModeFragment;
import org.blisslabs.inputctl.helpers.InputDeviceManagerHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TabletModeService extends Service {
    public static final String ACTION_TABLET_MODE_CHANGED = "org.blisslabs.inputctl.action.TABLET_MODE_CHANGED";
    public static final String EXTRA_IS_TABLET_MODE = "is_tablet_mode";

    private static final String TAG = "Inputctl_Service";
    
    private InputManager mInputManager;
    private InputDeviceManagerHelper mHelper;
    private SharedPreferences mPrefs;
    private Handler mHandler;

    private final InputManager.OnTabletModeChangedListener mTabletModeListener = 
            new InputManager.OnTabletModeChangedListener() {
        @Override
        public void onTabletModeChanged(long whenNanos, boolean inTabletMode) {
            Log.d(TAG, "Detect Tablet Mode changed: " + inTabletMode);
            handleTabletModeChange(inTabletMode);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TabletModeService is starting...");
        
        mHelper = new InputDeviceManagerHelper(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mHandler = new Handler(Looper.getMainLooper());
        mInputManager = getSystemService(InputManager.class);

        // Register tablet mode change
        if (mInputManager != null) {
            mInputManager.registerOnTabletModeChangedListener(mTabletModeListener, mHandler);
            Log.d(TAG, "Successfully registered OnTabletModeChangedListener!");
        }
    }

    private void handleTabletModeChange(boolean isTabletMode) {
        Set<String> targetDevices = mPrefs.getStringSet(TabletModeFragment.PREF_AUTO_DISABLE_DEVICES, new HashSet<>());
        Intent broadcastIntent = new Intent(ACTION_TABLET_MODE_CHANGED);
        broadcastIntent.putExtra(EXTRA_IS_TABLET_MODE, isTabletMode);
        sendBroadcast(broadcastIntent);
        if (targetDevices.isEmpty()) return;

        List<InputDevice> allDevices = mHelper.getPhysicalInputDevices();
        int affectedCount = 0;

        for (InputDevice device : allDevices) {
            if (targetDevices.contains(device.getName())) {
                if (isTabletMode) {
                    mHelper.disableDevice(device.getId());
                } else {
                    mHelper.enableDevice(device.getId());
                }
                affectedCount++;
            }
        }

        // u show toast ?
        boolean showToast = mPrefs.getBoolean("pref_tablet_mode_show_toast", true);
        if (showToast && affectedCount > 0) {
            String message = isTabletMode ? 
                    getString(R.string.toast_tablet_mode_disabled, affectedCount) : 
                    getString(R.string.toast_laptop_mode_enabled, affectedCount);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; 
    }

    @Override
    public void onDestroy() {
        if (mInputManager != null) {
            mInputManager.unregisterOnTabletModeChangedListener(mTabletModeListener);
            Log.d(TAG, "Unregistered TabletModeListener.");
        }
        super.onDestroy();
    }
}
