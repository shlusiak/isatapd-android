package de.saschahlusiak.isatapd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("enabled", false)) {
			Log.d("BootCompletedReceiver", "boot completed, isatapd enabled, starting");
			ISATAP.start_isatapd(context);
		}
	}

}
