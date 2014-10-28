package com.actionsmicro.analytics.usage;

import android.content.Context;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;

@SuppressWarnings("unused")
public class MediaUsage extends Usage {
	
	private String result;
	private int normalized_result;
	private long duration;
	private String title;

	public MediaUsage(Tracker tracker, Context context, String appId,
			String packageId, DeviceInfo device, String recordType,
			String schemaVersion) {
		super(tracker, context, appId, packageId, device, recordType, schemaVersion);
	}
	public MediaUsage(Tracker tracker, Context context, DeviceInfo device, String recordType,
			String schemaVersion) {
		super(tracker, context, device, recordType, schemaVersion);
	}
	public void setResult(String deviceNativeResult, int normalized_result) {
		this.result = deviceNativeResult;
		this.normalized_result = normalized_result;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public void setTitle(String title) {
		this.title = title;
	}

}
