package com.mmalk.RunningManager;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Fragment responsible for "linking" R.xml.preferences to Settings activity
 */

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
