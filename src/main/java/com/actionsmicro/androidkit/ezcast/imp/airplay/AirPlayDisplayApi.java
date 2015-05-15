package com.actionsmicro.androidkit.ezcast.imp.airplay;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.YuvImage;

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.graphics.YuvImageToJpegHelper;

public class AirPlayDisplayApi extends AirPlayApi implements DisplayApi {

	public AirPlayDisplayApi(DisplayApiBuilder apiBuilder) {
		super(apiBuilder);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void startDisplaying() {
		startTrackingWifiDisplay();
	}

	@Override
	public void stopDisplaying() {
		stopTrackingWifiDisplay();
	}

	@Override
	public void resendLastImage() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendJpegEncodedScreenData(InputStream input, long length)
			throws Exception {
		getAirPlayClient().displayPhoto(input, length);

	}

	@Override
	public void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception {
		YuvImageToJpegHelper helper = YuvImageToJpegHelper.getDefaultHelper();
		synchronized (helper) {
			InputStream inputStream = helper.compressYuvImageToJpegStream(yuvImage, quailty);
			sendJpegEncodedScreenData(inputStream, inputStream.available());
		}
	}
}
