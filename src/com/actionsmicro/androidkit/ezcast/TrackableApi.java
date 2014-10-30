package com.actionsmicro.androidkit.ezcast;

import android.content.Context;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.usage.LocalAudioUsage;
import com.actionsmicro.analytics.usage.LocalVideoUsage;
import com.actionsmicro.analytics.usage.MediaUsage;
import com.actionsmicro.analytics.usage.WebVideoUsage;
import com.actionsmicro.analytics.usage.WifiDisplayUsage;
import com.actionsmicro.pigeon.MediaStreamingFileDataSource;
import com.actionsmicro.utils.Utils;

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
		stopTrackingWifiDisplay();
		commitMediaUsageTracking();
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
	public void startTrackingWifiDisplay() {
		if (wifiDisplayUsage != null) {
			throw new IllegalStateException("startDisplaying is called more than once");
		}
		wifiDisplayUsage = (WifiDisplayUsage) new WifiDisplayUsage(getTracker(), getContext(), getDevice()).begin();
	}
	public void stopTrackingWifiDisplay() {
		if (wifiDisplayUsage != null) {
			wifiDisplayUsage.commit();
			wifiDisplayUsage = null;
		}
	}
	private MediaUsage mediaUsage;
	public synchronized void beginRemoteMediaUsageTracking(String mediaUriString,
			String userAgentString, String title) {
		if (title == null || title.length() == 0) {
			title = com.actionsmicro.utils.Utils.getLastPathComponent(mediaUriString);
		}
		if (mediaUsage != null) {
			throw new IllegalStateException("un-committed media usage exists.");
		}
		mediaUsage = (MediaUsage) new WebVideoUsage(getTracker(), getContext(), getDevice(), mediaUriString).setUserAgent(userAgentString).setTitle(title).begin();
	}
	public synchronized void beginLocalMediaUsageTracking(String url, String title) {
		if (MediaStreamingFileDataSource.isAudioFileExt(Utils.getFileExtension(url))) {
			beginLocalAudioUsageTracking(url, title);
		} else {
			beginLocalVideoUsageTracking(url, title);
		}
	}
	public synchronized void beginLocalAudioUsageTracking(String url, String title) {
		if (title == null || title.length() == 0) {
			title = com.actionsmicro.utils.Utils.getLastPathComponent(url);
		}
		if (mediaUsage != null) {
			throw new IllegalStateException("un-committed media usage exists.");
		}
		mediaUsage = (MediaUsage) new LocalAudioUsage(getTracker(), getContext(), getDevice()).setTitle(title).begin();
	}
	public synchronized void beginLocalVideoUsageTracking(String url, String title) {
		if (title == null || title.length() == 0) {
			title = com.actionsmicro.utils.Utils.getLastPathComponent(url);
		}
		if (mediaUsage != null) {
			throw new IllegalStateException("un-committed media usage exists.");
		}
		mediaUsage = (MediaUsage) new LocalVideoUsage(getTracker(), getContext(), getDevice()).setTitle(title).begin();
	}
	public synchronized void commitMediaUsageTracking() {
		if (mediaUsage != null) {
			mediaUsage.commit();
			mediaUsage = null;
		}
	}
	public synchronized void setMediaUsageResultCode(String resultString, int resultCode) {
		if (mediaUsage != null) {
			mediaUsage.setResult(resultString, resultCode);
		} else {
			throw new IllegalStateException("mediaUsage doesn't exist.");
		}
	}
	public synchronized void setMediaUsageDuration(int duration) {
		if (mediaUsage != null) {
			mediaUsage.setDuration(duration);
		} else {
			throw new IllegalStateException("mediaUsage doesn't exist.");
		}
	}
	
}
