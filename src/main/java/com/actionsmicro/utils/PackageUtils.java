package com.actionsmicro.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class PackageUtils {
	public static String getAppVersion(Context context) {
		String appVersion = "";
		try {
			appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return appVersion;
	}

	public static String getApplicationName(Context context) {
		ApplicationInfo applicationInfo = context.getApplicationInfo();
		int stringId = applicationInfo.labelRes;
		return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
	}
}
