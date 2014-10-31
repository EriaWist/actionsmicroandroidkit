package com.actionsmicro.analytics.usage;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;

import android.content.Context;

@SuppressWarnings("unused")
public class WebVideoUsage extends RemoteMediaUsage {
	public WebVideoUsage(Tracker tracker, Context context, String appId,
			String packageId, DeviceInfo device, String url) { 
		super(tracker, context, appId, packageId, device, "web_video", "2014-10-24", url);
	}
	public WebVideoUsage(Tracker tracker, Context context, DeviceInfo device, String url) { 
		super(tracker, context, device, "web_video", "2014-10-24", url);
	}
}
