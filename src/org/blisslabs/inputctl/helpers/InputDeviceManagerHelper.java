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

package org.blisslabs.inputctl.helpers;

import android.content.Context;
import android.hardware.input.InputManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;

public class InputDeviceManagerHelper {
    private static final String TAG = "Inputctl_Helper";
    private static final int SW_TABLET_MODE = 1; 
    
    private final InputManager mInputManager;

    public InputDeviceManagerHelper(Context context) {
        mInputManager = context.getSystemService(InputManager.class);
    }

    /**
     * Get a list of all physical input devices (ignore virtual devices).
     */
    public List<InputDevice> getPhysicalInputDevices() {
        int[] ids = mInputManager.getInputDeviceIds();
        List<InputDevice> devices = new ArrayList<>();
        
        for (int id : ids) {
            InputDevice device = mInputManager.getInputDevice(id);
            if (device != null && !device.isVirtual()) {
                devices.add(device);
            }
        }
        return devices;
    }

    /**
     * Disable the device immediately (Requires DISABLE_INPUT_DEVICE)
     */
    public void disableDevice(int deviceId) {
        try {
            Log.d(TAG, "Disabling device ID: " + deviceId);
            mInputManager.disableInputDevice(deviceId);
        } catch (SecurityException e) {
            Log.e(TAG, "Error! DISABLE_INPUT_DEVICE is not granted yet !", e);
        }
    }

    /**
     * Reactivate the device (Requires DISABLE_INPUT_DEVICE)
     */
    public void enableDevice(int deviceId) {
        try {
            Log.d(TAG, "Enabling device ID: " + deviceId);
            mInputManager.enableInputDevice(deviceId);
        } catch (SecurityException e) {
            Log.e(TAG, "Error! DISABLE_INPUT_DEVICE is not granted yet !", e);
        }
    }

    /**
     * Check if this device is currently disabled by the system.
     */
    public boolean isDeviceEnabled(int deviceId) {
        try {
            return mInputManager.isInputDeviceEnabled(deviceId);
        } catch (Exception e) {
            Log.e(TAG, "Error! Can't get device state !", e);
            return true;
        }
    }

    /**
     * Check if the device has a Tablet Mode recognition switch.
     */
    public boolean hasTabletModeSwitch(int deviceId) {
        int state = mInputManager.getSwitchState(deviceId, InputDevice.SOURCE_ANY, SW_TABLET_MODE);
        return state != -1; // Return -1 meaning KEY_STATE_UNKNOWN
    }

    /**
     * Check if the device has a Power Button.
     */
    public boolean hasPowerButton(int deviceId) {
        InputDevice device = mInputManager.getInputDevice(deviceId);
        if (device != null) {
            boolean[] hasKeys = device.hasKeys(new int[]{KeyEvent.KEYCODE_POWER});
            return hasKeys != null && hasKeys.length > 0 && hasKeys[0];
        }
        return false;
    }

    /**
     * Check if the device is folded into Tablet mode.
     */
    public boolean isCurrentlyInTabletMode() {
        int state = mInputManager.isInTabletMode();
        return state == 1;
    }
}

