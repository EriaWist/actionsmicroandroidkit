package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import android.graphics.YuvImage;

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.pigeon.Client;
import com.actionsmicro.pigeon.Client.OnNotificationListener;

import java.io.InputStream;

public class PigeonDisplayApi extends PigeonApi implements DisplayApi {

	private DisplayListener displayListener;
	private OnNotificationListener onNotificationListener;
	public PigeonDisplayApi(DisplayApiBuilder displayApiBuilder) {
		super(displayApiBuilder);
		displayListener = displayApiBuilder.getDisplayListener();
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
	protected void onPigeonClientCreated(Client pigeonClient) {
		pigeonClient.addOnNotificationListener(onNotificationListener = new OnNotificationListener() {

			@Override
			public void onRemoteRequestToStart(Client client,
											   int numberOfWindows, int position) {
				if (displayListener != null) {
					displayListener.remoteRequestToStartDisplaying(PigeonDisplayApi.this, numberOfWindows, position);
				}
			}

			@Override
			public void onRemoteRequestToStop(Client client) {
				if (displayListener != null) {
					displayListener.remoteRequestToStopDisplaying(PigeonDisplayApi.this);
				}
			}

			@Override
			public void onRemoteRequestToChangePostion(Client client,
													   int numberOfWindows, int position) {
				if (displayListener != null) {
					displayListener.positionDidChange(PigeonDisplayApi.this, numberOfWindows, position);
				}
			}

			@Override
			public void onRemoteRequestToDisconnect(Client client) {
				if (displayListener != null) {
					displayListener.remoteRequestToDisconnect(PigeonDisplayApi.this);
				}
			}

		});
	}
	@Override
	protected void onPigeonClientReleased(Client pigeonClient) {
		pigeonClient.removeOnNotificationListener(onNotificationListener);
	}

	@Override
	public void resendLastImage() throws Exception {
		if (pigeonClient != null) {
			pigeonClient.resendLastImage();
		}
	}
	@Override
	public void sendJpegEncodedScreenData(InputStream inputStream, long length) throws Exception { // TODO deal with parameter length
		if (pigeonClient != null) {
			pigeonClient.sendJpegStreamToServer(inputStream);
		}
	}

	@Override
	public void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception {
		if (pigeonClient != null) {
			pigeonClient.sendImageToServer(yuvImage, quailty);
		}
	}

	@Override
	public void sendH264EncodedScreenData(byte[] contents, int width, int height) throws Exception {
		if (pigeonClient != null) {
			pigeonClient.sendH264ImageBytesToServer(contents, width, height);
		}
	}
}
