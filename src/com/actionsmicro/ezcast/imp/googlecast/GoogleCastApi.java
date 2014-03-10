package com.actionsmicro.ezcast.imp.googlecast;

import android.content.Context;

import com.actionsmicro.ezcast.Api;
import com.actionsmicro.ezcast.ApiBuilder;
import com.actionsmicro.ezcast.ConnectionManager;
import com.actionsmicro.utils.Log;
import com.google.android.gms.cast.CastDevice;

public class GoogleCastApi implements Api{

	private static final String TAG = "GoogleCastApi";
	protected CastDevice castDevice;
	protected ConnectionManager connectionManager;
	private EZCastOverGoogleCast googleCastClient;
	protected synchronized EZCastOverGoogleCast getGoogleCastClient() {
		return googleCastClient;
	}

	protected Context context;
	private ConnectionManager connectionManagerProxy;

	public <T> GoogleCastApi(ApiBuilder<T> apiBuilder) {
		context = apiBuilder.getContext();
		connectionManager = apiBuilder.getConnectionManager();
		castDevice = ((GoogleCastDeviceInfo)apiBuilder.getDevice()).getCastDevice();		
	}

	@Override
	public synchronized void connect() {
		googleCastClient = EZCastOverGoogleCast.createClient(context, castDevice, connectionManagerProxy = new ConnectionManager() {

			@Override
			public void onConnectionFailed(Api api, Exception e) {
				if (connectionManager != null) {
					connectionManager.onConnectionFailed(GoogleCastApi.this, e);
				}
			}
			
		});
		if (googleCastClient == null) {
			Log.d(TAG, "googleCastClient is null");
		} else {
			onCreateGoogleCastClient(googleCastClient);
		}
	}

	protected void onCreateGoogleCastClient(EZCastOverGoogleCast googleCastClient) {
	}

	@Override
	public synchronized void disconnect() {
	
		if (googleCastClient != null) {
			EZCastOverGoogleCast.releaseClient(googleCastClient, connectionManagerProxy);	
			googleCastClient = null;
		}
	}

}