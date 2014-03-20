package com.actionsmicro.ezcast.imp.androidrx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import android.graphics.YuvImage;
import android.util.Log;

import com.actionsmicro.ezcast.DisplayApi;
import com.actionsmicro.ezcast.DisplayApiBuilder;
import com.actionsmicro.web.SimpleMotionJpegHttpServer;

public class AndroidRxDisplayApi extends AndroidRxApi implements DisplayApi {

	static final String TAG = "AndroidRxDisplayApi";
	private ByteArrayOutputStream compressionBuffer;

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
		//TODO DRY following code
		final int width = yuvImage.getWidth();
		final int height = yuvImage.getHeight();
		Log.d(TAG, "sentImageToServer width=" + width+",height=" + height);
		ByteArrayOutputStream compressionBuffer = getCompressionBuffer();
		compressionBuffer.reset();
		Log.d(TAG, "Start compress");
		android.graphics.Rect rect = new android.graphics.Rect(0, 0, width, height); 
		yuvImage.compressToJpeg(rect, quailty, compressionBuffer);
		sendJpegEncodedScreenData(new ByteArrayInputStream(compressionBuffer.toByteArray()), compressionBuffer.size());
		compressionBuffer.reset();
	}
	private ByteArrayOutputStream getCompressionBuffer() {
		// for performance reason we keep it as member
		if (compressionBuffer == null) {
			compressionBuffer = new ByteArrayOutputStream(1024*1024);
		}
		return compressionBuffer;
	}
}
