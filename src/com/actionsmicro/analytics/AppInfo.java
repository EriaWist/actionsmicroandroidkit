package com.actionsmicro.analytics;

import android.content.Context;
// POJO
@SuppressWarnings("unused")
public class AppInfo {
	private String package_id;

	public AppInfo(Context context) {
		package_id = context.getPackageName();
	}
}
