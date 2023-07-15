package com.chrislevis.carbcounter;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class CcPreferences extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.cc_prefs);
    }

}
