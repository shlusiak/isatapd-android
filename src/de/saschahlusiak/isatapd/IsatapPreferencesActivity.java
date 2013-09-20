package de.saschahlusiak.isatapd;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class IsatapPreferencesActivity extends PreferenceActivity {
	
	Handler handler = new Handler();
	
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
		
		Runnable runUpdateStatus = new Runnable() {
			@Override
			public void run() {
				updateStatus();
				handler.postDelayed(this, 2000);
			}
		};
		updateStatus();
		handler.postDelayed(runUpdateStatus, 100);
	}
	
	boolean toggleEnabled(boolean enabled) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String routers[] = { prefs.getString("routers", "") };
		try {
			if (enabled)
				ISATAP.start_isatapd(this,
						prefs.getString("interface", "is0"),
						Integer.parseInt(prefs.getString("mtu", "1280")),
						Integer.parseInt(prefs.getString("ttl", "64")),
						prefs.getBoolean("pmtudisc", true),
						routers,
						Integer.parseInt(prefs.getString("rsinterval", "0")),
						Integer.parseInt(prefs.getString("checkdns", "3600")));
			else
				ISATAP.stop_isatapd(this);
		} catch (IllegalStateException e) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(e.getMessage());
			builder.setTitle(android.R.string.dialog_alert_title);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.create().show();
			return false;
		}
		
		Runnable runUpdateStatus = new Runnable() {
			@Override
			public void run() {
				updateStatus();
			}
		};
		handler.postDelayed(runUpdateStatus, 300);

		return true;
	}	
	
	void updateStatus() {
		PreferenceScreen ps = (PreferenceScreen)findPreference("status");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String ifname = prefs.getString("interface", "is0");
		
		findPreference("status_title").setTitle(getString(R.string.status_title, ifname));
		ps.setTitle(R.string.interface_not_found);
		ps.setSummary("");
		
		/* TODO: support for listing multiple IPv6 addresses on the same ISATAP interface */
		/* TODO: show MTU? */
		try {
			NetworkInterface i = NetworkInterface.getByName(ifname);
			if (i != null) {
				ps.setTitle(R.string.interface_connecting);
				Enumeration<InetAddress> addresses = i.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr.isAnyLocalAddress())
						continue;
					if (addr.isLoopbackAddress())
						continue;
					if (addr.isMulticastAddress())
						continue;
					String s = addr.getHostAddress();
					if (s.contains("%"))
						s = s.substring(0, s.indexOf("%"));
					ps.setSummary(s);
					if (!addr.isLinkLocalAddress()) {
						ps.setTitle(R.string.interface_connected);
						break;
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
			ps.setTitle(R.string.interface_not_found);
			ps.setSummary(R.string.interface_error);
		}		
	}
}
