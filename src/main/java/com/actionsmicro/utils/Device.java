package com.actionsmicro.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

public class Device {

	public static final String DEVICE_TYPE_PHONE = "phone";
	public static final String DEVICE_TYPE_PAD = "pad";
	public static final String DEVICE_TYPE_TV = "tv";

	private static final String TAG = "Device";

	public static final String APP_UNIQUEID_PREF_KEY = "com.actionsmicro.appuuid";
	public static final String DEFAULT_WIFIAP_ADDRESS = "192.168.43.1";

	static public int getDeviceNaturlOrientation(Activity activity) {

		WindowManager windowManager = activity.getWindowManager();

		Configuration cfg = activity.getResources().getConfiguration();
		int lRotation = windowManager.getDefaultDisplay().getRotation();
		Log.d(TAG, "lRotation:"+lRotation+", cfg.orientation:"+cfg.orientation);
		DisplayMetrics dm = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(dm);
		Log.d(TAG, "dm.widthPixels:"+dm.widthPixels+", dm.heightPixels:"+dm.heightPixels);
		if( (((lRotation == Surface.ROTATION_0) ||(lRotation == Surface.ROTATION_180)) &&   
				(cfg.orientation == Configuration.ORIENTATION_LANDSCAPE)) ||
				(((lRotation == Surface.ROTATION_90) ||(lRotation == Surface.ROTATION_270)) &&    
						(cfg.orientation == Configuration.ORIENTATION_PORTRAIT))){

			return Configuration.ORIENTATION_LANDSCAPE;
		}     

		return Configuration.ORIENTATION_PORTRAIT;
	}

	static public boolean isTablet(Context context) {
		boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
		boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
		return (xlarge || large);
	}

	public static String deviceType(Context context) {
		Configuration config = context.getResources().getConfiguration();
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		// Verifies if the Generalized Size of the device is XLARGE to be
		// considered a Tablet
		boolean isXLarge = ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
		// If xlarge, checks density should be mdpi, hdpi, xhdpi
		if(isXLarge) {
			wm.getDefaultDisplay().getMetrics(metrics);
			// MDPI=160, DEFAULT=160, DENSITY_HIGH=240, DENSITY_MEDIUM=160,
			// DENSITY_TV=213, DENSITY_XHIGH=320
			if(metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT
					|| metrics.densityDpi == DisplayMetrics.DENSITY_HIGH
					|| metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM
					|| metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH) {
				return DEVICE_TYPE_PAD;
			}
		}
		// If large, checks density should be hdpi, tvdpi
		boolean isLarge = ((config.screenLayout &Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
		if(isLarge) {
			if(metrics.densityDpi == DisplayMetrics.DENSITY_HIGH) {
				return DEVICE_TYPE_PAD;
			}
			if (metrics.densityDpi == DisplayMetrics.DENSITY_TV) {
				return DEVICE_TYPE_TV;
			}
		}
		return DEVICE_TYPE_PHONE;
	}
	public static String getAppMacAddress(Context context) {
		return getAppUniqueId(context);
	}
	public static String getAppMacAddress(Context context, String defaultValue) {
		WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiMan != null) {
			WifiInfo wifiInf = wifiMan.getConnectionInfo();
			if (wifiInf != null) {
				String macAddress = wifiInf.getMacAddress();
				if (macAddress != null) {
					return macAddress;
				}
			}
		}
		return defaultValue;
	}
	public static void saveAppUniqueId(Context context, String macAdd) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(APP_UNIQUEID_PREF_KEY, macAdd);
		editor.commit();
	}
	public static String getAppUniqueId(Context context) {
		String uniqueId = getAppMacAddress(context, "02:00:00:00:00:00");
		if ("02:00:00:00:00:00".equals(uniqueId)) {
			uniqueId = getUUID(context, APP_UNIQUEID_PREF_KEY);
		}
		return uniqueId;
	}
	public static String getEZScreenServiceName(Context context, String preferenceKey) {
		String uuid = getUUID(context, preferenceKey);
		return "EZCastScreen-" + uuid.substring(uuid.length()-3);
	}
	public static String getUUID(Context context, String preferenceKey) {
		String uuidString = PreferenceManager.getDefaultSharedPreferences(context).getString(preferenceKey, ""); 
		if (uuidString.isEmpty()) {
			UUID uuid = UUID.randomUUID();
			uuidString = uuid.toString();
	        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putString(preferenceKey, uuidString);
			editor.commit();
		}
        return uuidString;
	}

	public static final String USB_AUDIO_AUTOMATIC_ROUTING_DISABLED =
			"usb_audio_automatic_routing_disabled";

	public static int isUsbAudioAutoDisabled(Context context) {
		int isDisabled = Settings.Secure.getInt(context.getContentResolver(),
				USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, 0);
		return isDisabled;
	}

	public static String getWifiApIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
					.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				if (intf.getName().startsWith("wlan") || intf.getName().startsWith("ap")) {
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
							.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()
								&& (inetAddress.getAddress().length == 4)) {
							Log.d(TAG, inetAddress.getHostAddress());
							return inetAddress.getHostAddress();
						}
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, ex.toString());
		}
		return DEFAULT_WIFIAP_ADDRESS;
	}

	public static String getHostIpAddress(Context context, boolean useIPv4) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();

		String ipString = String.format(Locale.US,
				"%d.%d.%d.%d",
				(ip & 0xff),
				(ip >> 8 & 0xff),
				(ip >> 16 & 0xff),
				(ip >> 24 & 0xff));

		if (Reachability.isWifiApEnabled(context)) {
			ipString = getWifiApIpAddress();
		}

		return ipString;
    }
}
