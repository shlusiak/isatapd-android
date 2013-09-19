package de.saschahlusiak.isatapd;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class IsatapPreferencesActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		ISATAP.installBinary(this);
		
		findPreference("enabled").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				return toggleEnabled((Boolean)newValue);
			}
		});
	}
	
	boolean toggleEnabled(boolean enabled) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String routers[] = { prefs.getString("routers", "") };
		
		if (enabled)
			ISATAP.start_isatapd(this, "is0", null, 0, ISATAP.TTL_DEFAULT, 1, routers, 0, ISATAP.DNS_CHECK_DEFAULT);
		else
			ISATAP.stop_isatapd(this);
		
		return true;
	}
}
