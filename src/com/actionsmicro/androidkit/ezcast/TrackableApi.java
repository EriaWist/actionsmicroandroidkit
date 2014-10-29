package com.actionsmicro.androidkit.ezcast;

import android.content.Context;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.usage.WifiDisplayUsage;

public abstract class TrackableApi implements Api {

	private EzCastSdk sdk;
	private DeviceInfo device;
	private Context context;
	private WifiDisplayUsage wifiDisplayUsage;

	public TrackableApi(ApiBuilder<?> apiBuilder) {
		sdk = apiBuilder.getSdk();
		device = apiBuilder.getDevice();
		context = apiBuilder.getContext();
	}
	@Override
	public void connect() {
		sdk.connectToDevice(device);
	}
	@Override
	public void disconnect() {
		sdk.disconnectFromDevice(device);
	}
	protected Tracker getTracker() {
		return sdk.getTracker();
	}
	protected DeviceInfo getDevice() {
		return device;
	}
	protected Context getContext() {
		return context;
	}
	public void startDisplaying() {
		if (wifiDisplayUsage != null) {
			throw new IllegalStateException("startDisplaying is called more than once");
		}
		wifiDisplayUsage = (WifiDisplayUsage) new WifiDisplayUsage(getTracker(), getContext(), getDevice()).begin();
	}
	public void stopDisplaying() {
		if (wifiDisplayUsage != null) {
			wifiDisplayUsage.commit();
		} else {
			throw new IllegalStateException("startDisplaying is not called");
		}
	}
}
