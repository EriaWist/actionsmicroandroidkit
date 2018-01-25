package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import java.io.InputStream;

import android.graphics.YuvImage;

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;

public class AndroidRxDisplayApi extends AndroidRxApi implements DisplayApi {

	static final String TAG = "AndroidRxDisplayApi";
	public AndroidRxDisplayApi(DisplayApiBuilder displayApiBuilder) {
		super(displayApiBuilder);
	}

	@Override
	public void startDisplaying() {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidRxClient().startDisplaying();
		startTrackingWifiDisplay();
	}
	

	@Override
	public void stopDisplaying() {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidRxClient().stopDisplaying();
		stopTrackingWifiDisplay();
	}	

	@Override
	public void resendLastImage() throws Exception {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidRxClient().resendLastImage();
	}

	@Override
	public synchronized void sendJpegEncodedScreenData(InputStream input, long length)
			throws Exception {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidRxClient().sendJpegEncodedScreenData(input, length);
	}

	@Override
	public synchronized void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception { 
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidRxClient().sendYuvScreenData(yuvImage, quailty);
		
	}

	@Override
	public void sendH264EncodedScreenData(byte[] contents, int width, int height) throws Exception {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidRxClient().sendH264EncodedScreenData(contents,width,height);
	}

	@Override
	public void setPinCode(String code) throws Exception {

	}
}
