package com.actionsmicro.analytics.usage;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;

import android.content.Context;

public class LocalAudioUsage extends MediaUsage {
	public LocalAudioUsage(Tracker tracker, Context context, String appId,
			String packageId, DeviceInfo device) {
		super(tracker, context, appId, packageId, device, "local_audio", "2014-10-24");
	}

	public LocalAudioUsage(Tracker tracker, Context context, DeviceInfo device) {
		super(tracker, context, device, "local_audio", "2014-10-24");
	}
}
