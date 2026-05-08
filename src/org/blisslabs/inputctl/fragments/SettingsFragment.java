package org.blisslabs.inputctl.fragments;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import org.blisslabs.inputctl.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.general_settings, rootKey);
    }
}
