package com.actionsmicro.analytics.usage;

import android.content.Context;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;

public class WebAudioUsage extends RemoteMediaUsage {

	public WebAudioUsage(Tracker tracker, Context context, String appId,
			String packageId, DeviceInfo device,  String url) {
		super(tracker, context, appId, packageId, device, "web_audio", "2014-10-24",
				url);
	}
	public WebAudioUsage(Tracker tracker, Context context, DeviceInfo device, String url) {
		super(tracker, context, device, "web_audio", "2014-10-24", url);
	}
}
