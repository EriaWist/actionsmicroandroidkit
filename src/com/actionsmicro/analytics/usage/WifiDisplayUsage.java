package com.actionsmicro.analytics.usage;

import android.content.Context;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;

public class WifiDisplayUsage extends Usage {

	public WifiDisplayUsage(Tracker tracker, Context context, String appId,
			String packageId, DeviceInfo device) {
		super(tracker, context, appId, packageId, device, "wifi_display", "2014-10-24");
	}
	public WifiDisplayUsage(Tracker tracker, Context context, DeviceInfo device) {
		super(tracker, context, device, "wifi_display", "2014-10-24");
	}
}
