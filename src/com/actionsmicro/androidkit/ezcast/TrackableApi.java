package com.actionsmicro.androidkit.ezcast;

import java.io.File;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.actionsmicro.BuildConfig;
import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.usage.LocalAudioUsage;
import com.actionsmicro.analytics.usage.LocalVideoUsage;
import com.actionsmicro.analytics.usage.MediaUsage;
import com.actionsmicro.analytics.usage.WebVideoUsage;
import com.actionsmicro.analytics.usage.WifiDisplayUsage;
import com.actionsmicro.pigeon.MediaStreamingFileDataSource;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;

public abstract class TrackableApi implements Api {

	private static final String TAG = null;
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
			Log.e(TAG, "startDisplaying is called more than once");
			stopTrackingWifiDisplay();
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
	private synchronized void beginRemoteMediaUsageTracking(String mediaUriString,
			String userAgentString, String title) {
		if (title == null || title.length() == 0) {
			title = com.actionsmicro.utils.Utils.getLastPathComponent(mediaUriString);
		}
		if (mediaUsage != null) {
			if (BuildConfig.DEBUG) {
				throw new IllegalStateException("un-committed media usage exists.");
			}
		}
		mediaUsage = (MediaUsage) new WebVideoUsage(getTracker(), getContext(), getDevice(), mediaUriString).setUserAgent(userAgentString).setTitle(title).begin();
	}	
	private synchronized void beginLocalAudioUsageTracking(String url, String title) {
		if (title == null || title.length() == 0) {
			title = com.actionsmicro.utils.Utils.getLastPathComponent(url);
		}
		if (mediaUsage != null) {
			if (BuildConfig.DEBUG) {
				throw new IllegalStateException("un-committed media usage exists.");
			}
		}
		mediaUsage = (MediaUsage) new LocalAudioUsage(getTracker(), getContext(), getDevice()).setTitle(title).begin();
	}
	private synchronized void beginLocalVideoUsageTracking(String url, String title) {
		if (title == null || title.length() == 0) {
			title = com.actionsmicro.utils.Utils.getLastPathComponent(url);
		}
		if (mediaUsage != null) {
			if (BuildConfig.DEBUG) {
				throw new IllegalStateException("un-committed media usage exists.");
			}
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
			if (BuildConfig.DEBUG) {
				throw new IllegalStateException("mediaUsage doesn't exist.");
			}
		}
	}
	public synchronized void setMediaUsageDuration(int duration) {
		if (mediaUsage != null) {
			mediaUsage.setDuration(duration);
		} else {
			if (BuildConfig.DEBUG) {
				throw new IllegalStateException("mediaUsage doesn't exist.");
			}
		}
	}
	public void beginMediaUsageTracking(Context context, String url,
			String userAgentString, String title) {
		Uri mediaUri = null;
		try {
			mediaUri = Uri.parse(url);
			if (mediaUri.getScheme() == null) {
				mediaUri = mediaUri.buildUpon().scheme("file").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			mediaUri = Uri.fromFile(new File(url));
		}
		if (mediaUri.getScheme().equalsIgnoreCase(ContentResolver.SCHEME_CONTENT)) {
			String mimeType = context.getContentResolver().getType(mediaUri);
			if (mimeType != null && mimeType.startsWith("audio")) {
				beginLocalAudioUsageTracking(url, title);
			} else {
				beginLocalVideoUsageTracking(url, title);
			}
		} else if (mediaUri.getScheme().equalsIgnoreCase("file")) {
			if (MediaStreamingFileDataSource.isAudioFileExt(Utils.getFileExtension(url))) {
				beginLocalAudioUsageTracking(url, title);				
			} else {
				beginLocalVideoUsageTracking(url, title);
			}
		} else {
			beginRemoteMediaUsageTracking(url, userAgentString, title);
		}
	}
	
}
