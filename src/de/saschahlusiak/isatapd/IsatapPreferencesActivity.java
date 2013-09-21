package de.saschahlusiak.isatapd;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.BaseAdapter;
import android.widget.Toast;

public class IsatapPreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
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
		
		updateStatus();
	}
	
	@Override
	protected void onResume() {
		Runnable runUpdateStatus = new Runnable() {
			@Override
			public void run() {
				updateStatus();
				handler.postDelayed(this, 2000);
			}
		};
		handler.postDelayed(runUpdateStatus, 100);
		
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		onSharedPreferenceChanged(prefs, "routers");
		onSharedPreferenceChanged(prefs, "interface");
		onSharedPreferenceChanged(prefs, "mtu");
		onSharedPreferenceChanged(prefs, "ttl");
		onSharedPreferenceChanged(prefs, "rsinterval");
		onSharedPreferenceChanged(prefs, "checkdns");

		prefs.registerOnSharedPreferenceChangeListener(this);
		
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
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
		PreferenceScreen ps = (PreferenceScreen)findPreference("status_pref");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String ifname = prefs.getString("interface", "is0");
		
		findPreference("status_title").setTitle(getString(R.string.status_title, ifname));
		ps.setSummary(R.string.interface_not_found);
		ps.setEnabled(false);
		findPreference("status_mtu").setSummary(R.string.unknown);
		findPreference("status_interface").setSummary(ifname);
		findPreference("status_ll").setSummary(R.string.unknown);
		findPreference("status_global").setSummary(R.string.unknown);
		
		/* TODO: support for listing multiple IPv6 addresses on the same ISATAP interface */
		try {
			NetworkInterface i = NetworkInterface.getByName(ifname);
			if (i != null) {
				ps.setEnabled(true);
				findPreference("status_mtu").setSummary(getString(R.string.mtu_format, i.getMTU()));
				ps.setSummary(R.string.unknown);
				findPreference("status_ll").setSummary(R.string.unknown);
				findPreference("status_global").setSummary(R.string.unknown);
				Enumeration<InetAddress> addresses = i.getInetAddresses();
				boolean global = false;
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
					if (addr.isLinkLocalAddress()) {
						findPreference("status_ll").setSummary(s);
						if (!global)
							ps.setSummary(getString(R.string.interface_connecting, s));
					} else {
						global = true;
						findPreference("status_global").setSummary(s);
						ps.setSummary(getString(R.string.interface_connected, s));
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
			ps.setSummary(R.string.interface_error);
		}
		((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals("routers") ||
			key.equals("interface") ||
			key.equals("ttl") ||
			key.equals("mtu") ||
			key.equals("rsinterval") ||
			key.equals("checkdns"))
		{
			EditTextPreference pref = (EditTextPreference)findPreference(key);
			pref.setSummary(pref.getText());
		}
	}
}
