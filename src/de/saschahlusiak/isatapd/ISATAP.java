package de.saschahlusiak.isatapd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ISATAP {

	public static int TTL_DEFAULT = 64;
	public static int DNS_CHECK_DEFAULT = 3600;
	
	public static boolean start_isatapd(
			Context context,
			String if_name,				/* default: is0 */
			int mtu, 					/* default: auto */
			int ttl,					/* TTL_DEFAULT */
			boolean pmtudisc,			/* default: true */
			String routers[],			/* default: isatap */
			int rs_interval,			/* default: auto */
			int dns_recheck_interval	/* DNS_CHECK_DEFAULT */
		)
	{		
		try {
			String CMD = "";
			
			CMD += new File(context.getCacheDir(), "isatapd").getAbsolutePath();
			if (! if_name.equals(""))
				CMD += " --name " + if_name;
			if (ttl > 0)
				CMD += " --ttl " + ttl;
			else
				CMD += " --ttl inherit";
			CMD += " --check-dns " + dns_recheck_interval;
			CMD += " --pid " + new File(context.getCacheDir(), "isatapd.pid").getAbsolutePath();
			CMD += " --daemon";
			if (!pmtudisc)
				CMD += " --nopmtudisc";
			
			if (mtu > 0)
				CMD += " --mtu " + mtu;
			if (rs_interval > 0)
				CMD += " --interval " + rs_interval;
			for (int i = 0; i < routers.length; i++)
				CMD += " " + routers[i];
			
			Process p = Runtime.getRuntime().exec(new String[] {"su", "-c", CMD} );
			BufferedReader e = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			p.waitFor();
			String error = "", s;
			s = e.readLine();
			while (s != null) {
				error += s + "\n";
				s = e.readLine();
			}
			if (p.exitValue() != 0)
				throw new IllegalStateException(error);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
	}
	
	public static boolean start_isatapd(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		String routers[] = { prefs.getString("routers", "") };
		int mtu = 0;
		int ttl = 0;
		int rsinterval = 0;
		int checkdns = 0;
		try { mtu = Integer.parseInt(prefs.getString("mtu", "1280")); } catch (Exception e) {}
		try { ttl = Integer.parseInt(prefs.getString("ttl", "64")); } catch (Exception e) {}
		try { rsinterval = Integer.parseInt(prefs.getString("rsinterval", "0")); } catch (Exception e) {}
		try { checkdns = Integer.parseInt(prefs.getString("checkdns", "3600")); } catch (Exception e) {}
		return ISATAP.start_isatapd(context,
				prefs.getString("interface", "is0"),
				mtu,
				ttl,
				prefs.getBoolean("pmtudisc", true),
				routers,
				rsinterval,
				checkdns);
	}
	
	public static boolean stop_isatapd(Context context) {
		String CMD[] = new String[] {
				"su", "-c",
				"kill `cat " + new File(context.getCacheDir(), "isatapd.pid").getAbsolutePath() + "`" }; 
		try {
			Runtime.getRuntime().exec(CMD).waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static boolean send_hup(Context context) {
		String CMD[] = new String[] {
				"su", "-c",
				"kill -HUP `cat " + new File(context.getCacheDir(), "isatapd.pid").getAbsolutePath() + "`" }; 
		try {
			Runtime.getRuntime().exec(CMD).waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public static boolean installBinary(Context context) {
		File outFile = new File(context.getCacheDir(), "isatapd");
		OutputStream myOutput;
		
		try {
			myOutput = new FileOutputStream(outFile);
			InputStream is = context.getAssets().open("isatapd");
			if (is == null)
				return false;
							
			byte[] buffer = new byte[1024 * 8];
			int length;
			while ((length = is.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}
			is.close();
			myOutput.flush();
			myOutput.close();
			outFile.setExecutable(true, false);
		} catch (IOException e) {
			return false;
		}
	
		return true;
	}

}
