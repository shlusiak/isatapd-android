package de.saschahlusiak.isatapd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;

public class ISATAP {

	public static int TTL_DEFAULT = 64;
	public static int DNS_CHECK_DEFAULT = 3600;
	
	public static boolean start_isatapd(
			Context context,
			String if_name,				/* default: is0 */
			String link, 				/* default: auto */
			int mtu, 					/* default: auto */
			int ttl,					/* TTL_DEFAULT */
			int pmtudisc,				/* default: true */
			String routers[],			/* default: isatap */
			int rs_interval,			/* default: auto */
			int dns_recheck_interval	/* DNS_CHECK_DEFAULT */
		)
	{		
		try {
			String CMD = "";
			
			CMD += new File(context.getCacheDir(), "isatapd").getAbsolutePath();
			CMD += " --name " + if_name;
			CMD += " --ttl " + ttl;
			CMD += " --check-dns " + dns_recheck_interval;
			CMD += " --pid " + new File(context.getCacheDir(), "isatapd.pid").getAbsolutePath();
			CMD += " --daemon";
			
			if (mtu > 0)
				CMD += " --mtu " + mtu;
			CMD += " --ttl " + ttl;
			if (rs_interval > 0)
				CMD += " --interval " + rs_interval;
			for (int i = 0; i < routers.length; i++)
				CMD += " " + routers[i];
			
			Runtime.getRuntime().exec(new String[] {"su", "-c", CMD} ).waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
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

	static {
//		System.loadLibrary("isatap");
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
