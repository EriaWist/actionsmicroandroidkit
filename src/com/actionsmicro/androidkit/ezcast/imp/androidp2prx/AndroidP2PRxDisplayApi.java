package com.actionsmicro.androidkit.ezcast.imp.androidp2prx;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.YuvImage;

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;

public class AndroidP2PRxDisplayApi extends AndroidP2PRxApi implements DisplayApi {

	static final String TAG = "AndroidP2PRxDisplayApi";
	public AndroidP2PRxDisplayApi(DisplayApiBuilder displayApiBuilder) {
		super(displayApiBuilder);
	}

	@Override
	public void startDisplaying() {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidP2PRxClient().startDisplaying();
		startTrackingWifiDisplay();
	}
	

	@Override
	public void stopDisplaying() {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidP2PRxClient().stopDisplaying();
		stopTrackingWifiDisplay();
	}	

	@Override
	public void resendLastImage() throws Exception {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidP2PRxClient().resendLastImage();
	}

	@Override
	public synchronized void sendJpegEncodedScreenData(InputStream input, long length)
			throws Exception {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidP2PRxClient().sendJpegEncodedScreenData(input, length);
	}

	@Override
	public synchronized void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception { 
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidP2PRxClient().sendYuvScreenData(yuvImage, quailty);
		
	}

	public void sendAudioEncodedData(InputStream inputStream)
			throws IllegalArgumentException, IOException {
	}
}
