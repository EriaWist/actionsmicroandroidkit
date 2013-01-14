package com.actionsmicro.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

public class Reachability {
	private static Method isWifiApEnabledMethod;
	/**
	 * Determine whether current device has network connection
	 * @param context
	 * @return 
	 */
	public static boolean hasNetworkConnection(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (null != cm) {
			return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
		}
		return false;
	}
	public static boolean hasWifiConnection(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (null != wifiManager) {
			return wifiManager.getConnectionInfo().getIpAddress() != 0;
		}
		return false;
	}
	public static boolean isWifiEnabled(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (null != wifiManager) {
			return wifiManager.isWifiEnabled();
		}
		return false;
	}
	public static boolean isWifiApEnabled(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (isWifiApEnabledMethod == null) {
			try {
				isWifiApEnabledMethod = wifiManager.getClass().getMethod("isWifiApEnabled", (Class<?>[])null);
				if (isWifiApEnabledMethod.getGenericReturnType() != boolean.class) {
					isWifiApEnabledMethod = null;
				}
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		if (isWifiApEnabledMethod != null) {
			try {
				return ((Boolean)isWifiApEnabledMethod.invoke(wifiManager, (Object[])null)).booleanValue();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		} else {
			return false;
		}
	}
}
