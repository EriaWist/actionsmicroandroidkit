package com.actionsmicro.analytics.usage;

import android.content.Context;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;

public class LocalVideoUsage extends MediaUsage {

	public LocalVideoUsage(Tracker tracker, Context context, String appId,
			String packageId, DeviceInfo device) {
		super(tracker, context, appId, packageId, device, "local_video", "2014-10-24");
	}

	public LocalVideoUsage(Tracker tracker, Context context, DeviceInfo device) {
		super(tracker, context, device, "local_video", "2014-10-24");
	}
}
