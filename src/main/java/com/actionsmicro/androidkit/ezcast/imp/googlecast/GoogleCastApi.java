package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.ConnectionManager;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.TrackableApi;
import com.actionsmicro.androidkit.ezcast.imp.googlecast.ScreenPresentation.BackPressedHandler;
import com.actionsmicro.utils.Log;

import java.io.InputStream;

public class GoogleCastApi extends TrackableApi implements Api{

	private static final String TAG = "GoogleCastApi";
	protected ConnectionManager connectionManager;
	private EZCastOverGoogleCast googleCastClient;
	private Bitmap mAdvertiseImage;
	private BackPressedHandler mBackPressedHandler;

	protected synchronized EZCastOverGoogleCast getGoogleCastClient() {
		return googleCastClient;
	}

	protected Context context;
	private ConnectionManager connectionManagerProxy;
    private DeviceInfo device;

	public <T> GoogleCastApi(ApiBuilder<T> apiBuilder) {
		super(apiBuilder);
		context = apiBuilder.getContext();
		connectionManager = apiBuilder.getConnectionManager();
		device = apiBuilder.getDevice();
	}

	@Override
	public synchronized void connect() {
		googleCastClient = EZCastOverGoogleCast.createClient(context, device, this, connectionManagerProxy = new ConnectionManager() {
			@Override
			public void onConnectionFailed(Api api, Exception e) {
				if (connectionManager != null) {
					connectionManager.onConnectionFailed(GoogleCastApi.this, e);
				}
			}
			
		}, mAdvertiseImage,mBackPressedHandler);
		if (googleCastClient == null) {
			Log.d(TAG, "googleCastClient is null");
		} else {
			onCreateGoogleCastClient(googleCastClient);
		}
		super.connect();
	}

	public void setAdvertiseImage(InputStream stream){
		mAdvertiseImage = BitmapFactory.decodeStream(stream);
	}

	public void setBackPressedHandler(BackPressedHandler backPressedHandler){
		mBackPressedHandler= backPressedHandler;
	}

	protected void onCreateGoogleCastClient(EZCastOverGoogleCast googleCastClient) {
	}

	@Override
	public synchronized void disconnect() {
	
		if (googleCastClient != null) {
			EZCastOverGoogleCast.releaseClient(googleCastClient, connectionManagerProxy);	
			googleCastClient = null;
		}
		super.disconnect();
	}

}