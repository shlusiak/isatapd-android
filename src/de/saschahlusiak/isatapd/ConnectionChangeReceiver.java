package de.saschahlusiak.isatapd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ConnectionChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("enabled", false)) {
			Log.d("ConnectionChange", "connection changed, isatapd enabled, sending HUP");
			ISATAP.send_hup(context);
		}
	}
	
}
