package com.actionsmicro.androidkit.ezcast.imp.airplay;

import java.io.InputStream;

import android.graphics.YuvImage;

import com.actionsmicro.airplay.AirPlayClient;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.androidkit.ezcast.EzCastSdk;
import com.actionsmicro.graphics.YuvImageToJpegHelper;
import com.actionsmicro.utils.Log;

public class AirPlayDisplayApi extends AirPlayApi implements DisplayApi {

	private DisplayListener displayListener;
	public AirPlayDisplayApi(DisplayApiBuilder apiBuilder) {
		super(apiBuilder);
		displayListener = apiBuilder.getDisplayListener();
	}

	@Override
	public void startDisplaying() {
		getAirPlayClient().CheckAuthState(displayListener, this);
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

	@Override
	public void sendH264EncodedScreenData(byte[] contents, int width, int height) throws Exception {

	}

	@Override
	public void setPinCode(String code) throws Exception {
		getAirPlayClient().SetPinCode(code);
	}
}
