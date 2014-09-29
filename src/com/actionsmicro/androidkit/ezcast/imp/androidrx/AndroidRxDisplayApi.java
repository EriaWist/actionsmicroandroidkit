package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import java.io.InputStream;
import java.util.HashMap;

import android.graphics.YuvImage;

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.graphics.YuvImageToJpegHelper;
import com.actionsmicro.web.SimpleMotionJpegHttpServer;

public class AndroidRxDisplayApi extends AndroidRxApi implements DisplayApi {

	static final String TAG = "AndroidRxDisplayApi";
	public AndroidRxDisplayApi(DisplayApiBuilder displayApiBuilder) {
		super(displayApiBuilder);
	}
	private static SimpleMotionJpegHttpServer simpleMotionJpegHttpServer;

	@Override
	public void startDisplaying() {
		final HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("url", getMjpegServer().getServerUrl());
		invokeRpcMethod("display", params);
	}
	private SimpleMotionJpegHttpServer getMjpegServer() {
		synchronized (this) {
			if (simpleMotionJpegHttpServer == null) {
				simpleMotionJpegHttpServer = new SimpleMotionJpegHttpServer(getContext(), 0, new SimpleMotionJpegHttpServer.OnConnectionListener() {
					@Override
					public void onClientConnected(SimpleMotionJpegHttpServer simpleMotionJpegHttpServer) {
					}
				});
			}
			return simpleMotionJpegHttpServer;
		}
	}

	@Override
	public void stopDisplaying() {
		invokeRpcMethod("stop_display", 3000);
		synchronized (this) {
			if (simpleMotionJpegHttpServer != null) {
				simpleMotionJpegHttpServer.cleanup();
				simpleMotionJpegHttpServer = null;
			}
		}
	}	

	@Override
	public void resendLastImage() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized void sendJpegEncodedScreenData(InputStream input, long length)
			throws Exception {
		SimpleMotionJpegHttpServer mjpegServer = getMjpegServer();
		if (mjpegServer != null) {
			mjpegServer.sendJpegStream(input, length);
		}
	}

	@Override
	public synchronized void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception { 
		YuvImageToJpegHelper helper = YuvImageToJpegHelper.getDefaultHelper();
		synchronized (helper) {
			InputStream inputStream = helper.compressYuvImageToJpegStream(yuvImage, quailty);
			sendJpegEncodedScreenData(inputStream, inputStream.available());
		}
	}
}
