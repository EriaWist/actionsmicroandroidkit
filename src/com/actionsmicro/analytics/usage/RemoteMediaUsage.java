package com.actionsmicro.analytics.usage;

import android.content.Context;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;

@SuppressWarnings("unused")
public class RemoteMediaUsage extends MediaUsage {

	private String url;
	private String user_agent;
	
	public RemoteMediaUsage(Tracker tracker, Context context, String appId,
			String packageId, DeviceInfo device, String recordType,
			String schemaVersion, String url) {
		super(tracker, context, appId, packageId, device, recordType,
				schemaVersion);
		this.url = url;

	}

	public RemoteMediaUsage(Tracker tracker, Context context,
			DeviceInfo device, String recordType, String schemaVersion, String url) {
		super(tracker, context, device, recordType, schemaVersion);
		this.url = url;
	}
	public void setUserAgent(String user_agent) {
		this.user_agent = user_agent;
	}

}