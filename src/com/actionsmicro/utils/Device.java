package com.actionsmicro.utils;

import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

public class Device {

	public static final String DEVICE_TYPE_PHONE = "phone";
	public static final String DEVICE_TYPE_PAD = "pad";
	public static final String DEVICE_TYPE_TV = "tv";
	
	private static final String TAG = "Device";

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
		WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiMan == null) {
			return "";
		}
		WifiInfo wifiInf = wifiMan.getConnectionInfo();
		return wifiInf.getMacAddress().toString();
	}
	public static String getEZScreenServiceName(Context context, String preferenceKey) {
//		String macAdd = getAppMacAddress(context);
//		macAdd = macAdd.replace(":", "");
//		String mac3End = macAdd.substring(macAdd.length() - 3);
//		Integer hex = Integer.parseInt(mac3End, 16 );
//		return "EZCastScreen" + hex;
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
}
