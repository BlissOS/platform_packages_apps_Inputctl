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
import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.blisslabs.inputctl.R;
import org.blisslabs.inputctl.helpers.KbdConfigHelper.KbdActionConfig;

import java.util.ArrayList;
import java.util.List;

public class KeyCaptureDialogFragment extends DialogFragment {

    public interface KeyCaptureListener {
        void onKeyCaptured(KbdActionConfig config);
        void onKeyCleared();
    }

    private KeyCaptureListener mListener;
    private TextView mTvCapturedKeys;

    private static class PressedKey {
        int scanCode;
        int keyCode;
        PressedKey(int sc, int kc) {
            this.scanCode = sc;
            this.keyCode = kc;
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PressedKey) return ((PressedKey) obj).scanCode == this.scanCode;
            return false;
        }
    }

    private KbdActionConfig mCurrentConfig = new KbdActionConfig();
    private List<PressedKey> mPressedKeys = new ArrayList<>();

    public void setListener(KeyCaptureListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_key_capture, null);

        mTvCapturedKeys = view.findViewById(R.id.tv_captured_keys);

        Button btnClear = view.findViewById(R.id.btn_clear);
        Button btnSave = view.findViewById(R.id.btn_save);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnClear.setOnClickListener(v -> {
            if (mListener != null) mListener.onKeyCleared();
            dismiss();
        });

        btnSave.setOnClickListener(v -> {
            if (mListener != null) mListener.onKeyCaptured(mCurrentConfig);
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        builder.setTitle(R.string.kbd_sensor_config_capture_title);

        Dialog dialog = builder.create();
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dismiss();
                return true;
            }
            
            PressedKey pk = new PressedKey(event.getScanCode(), event.getKeyCode());
            
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0 && !mPressedKeys.contains(pk) && mPressedKeys.size() < 3) {
                    mPressedKeys.add(pk);
                    updateConfigFromPressedKeys();
                }
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                mPressedKeys.remove(pk);
                return true;
            }
            return false;
        });

        return dialog;
    }

    private void updateConfigFromPressedKeys() {
        mCurrentConfig.key = -1;
        mCurrentConfig.mod1 = -1;
        mCurrentConfig.mod2 = -1;

        if (mPressedKeys.size() > 0) mCurrentConfig.key = mPressedKeys.get(mPressedKeys.size() - 1).scanCode;
        if (mPressedKeys.size() > 1) mCurrentConfig.mod1 = mPressedKeys.get(0).scanCode;
        if (mPressedKeys.size() > 2) mCurrentConfig.mod2 = mPressedKeys.get(1).scanCode;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mPressedKeys.size(); i++) {
            if (i > 0) sb.append(" + ");
            String keyName = KeyEvent.keyCodeToString(mPressedKeys.get(i).keyCode);
            if (keyName.startsWith("KEYCODE_")) keyName = keyName.substring(8);
            sb.append(keyName);
        }
        
        if (mPressedKeys.isEmpty()) {
            sb.append(getString(R.string.kbd_sensor_config_not_mapped));
        }

        mTvCapturedKeys.setText(sb.toString());
    }
}
