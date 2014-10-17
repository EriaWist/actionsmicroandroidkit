package com.actionsmicro.analytics;

import android.content.Context;

@SuppressWarnings("unused")
public abstract class Usage {
	private Tracker tracker;
	private Context context;
	private long timestamp;
	private String type;
	private String package_id;
	public Usage(Tracker tracker, Context context, String type) {
		this.tracker = tracker;
		this.context = context;
		this.type = type;
		this.package_id = context.getPackageName();
		// TODO gather app info
//		"app_id":"app_mac_address | UUID",
//		"package_id": "com.winnerwave.EZCast",
//		"device_type": "device_type",
//		"device_id":"device_mac_address",
//		and so on.
		
	}
	protected Tracker getTracker() {
		return tracker;
	}
	protected Context getContext() {
		return context;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void begin() {
		this.timestamp = System.currentTimeMillis();
	}
	public void commit() {
		tracker.log(this);
	}
}
