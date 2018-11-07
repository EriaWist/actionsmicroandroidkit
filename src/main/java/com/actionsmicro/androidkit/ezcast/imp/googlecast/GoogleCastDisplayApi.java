package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import java.io.InputStream;

import android.graphics.YuvImage;

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.graphics.YuvImageToJpegHelper;
import com.actionsmicro.utils.Log;

public class GoogleCastDisplayApi extends GoogleCastApi implements DisplayApi {

	protected static final String TAG = "GoogleCastDisplayApi";
	public GoogleCastDisplayApi(DisplayApiBuilder displayApiBuilder) {
		super(displayApiBuilder);
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
		YuvImageToJpegHelper helper = YuvImageToJpegHelper.getDefaultHelper();
		synchronized (helper) {
			InputStream inputStream = helper.compressYuvImageToJpegStream(yuvImage, quailty);
			sendJpegEncodedScreenData(inputStream, inputStream.available());
		}
	}

	@Override
	public void sendH264EncodedScreenData(byte[] contents, int width, int height) throws Exception {
		getGoogleCastClient().sendH264EncodedScreenData(contents,width,height);
	}
}
