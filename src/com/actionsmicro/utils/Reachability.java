package com.actionsmicro.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

public class Reachability {
	/**
	 * Determine whether current device has network connection
	 * @param context
	 * @return 
	 */
	static public boolean hasNetworkConnection(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (null != cm) {
			return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
		}
		return false;
	}
	static public boolean hasWifiConnection(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (null != wifiManager) {
			return wifiManager.getConnectionInfo().getIpAddress() != 0;
		}
		return false;
	}
}
