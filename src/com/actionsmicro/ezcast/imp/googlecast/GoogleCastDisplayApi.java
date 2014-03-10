package com.actionsmicro.ezcast.imp.googlecast;

import java.io.InputStream;

import android.graphics.YuvImage;

import com.actionsmicro.ezcast.DisplayApi;
import com.actionsmicro.ezcast.DisplayApiBuilder;

public class GoogleCastDisplayApi extends GoogleCastApi implements DisplayApi {

	protected static final String TAG = "GoogleCastDisplayApi";
	private DisplayListener displayListener;
	public GoogleCastDisplayApi(DisplayApiBuilder displayApiBuilder) {
		super(displayApiBuilder);
		displayListener = displayApiBuilder.getDisplayListener();
	}
	@Override
	public void startDisplaying() {
		getGoogleCastClient().startDisplaying();
	}

	@Override
	public void stopDisplaying() {
		getGoogleCastClient().stopDisplaying();
	}

	@Override
	public void resendLastImage() throws Exception {
		getGoogleCastClient().resendLastImage();
	}

	@Override
	public void sendJpegEncodedScreenData(InputStream input, long length)
			throws Exception {
		getGoogleCastClient().sendJpegEncodedScreenData(input, length);		
	}

	@Override
	public void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception {
		getGoogleCastClient().sendYuvScreenData(yuvImage, quailty);

	}
}
