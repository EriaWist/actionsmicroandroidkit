package com.actionsmicro.ezcast.imp.googlecast;

import android.content.Context;

import com.actionsmicro.ezcast.Api;
import com.actionsmicro.ezcast.ApiBuilder;
import com.actionsmicro.ezcast.ConnectionManager;
import com.google.android.gms.cast.CastDevice;

public class GoogleCastApi implements Api{

	protected CastDevice castDevice;
	protected ConnectionManager connectionManager;
	protected EZCastOverGoogleCast googleCastClient;
	protected Context context;

	public <T> GoogleCastApi(ApiBuilder<T> apiBuilder) {
		context = apiBuilder.getContext();
		connectionManager = apiBuilder.getConnectionManager();
		castDevice = ((GoogleCastDeviceInfo)apiBuilder.getDevice()).getCastDevice();		
	}

	@Override
	public void connect() {
		googleCastClient = EZCastOverGoogleCast.createClient(context, castDevice, connectionManager);
		onCreateGoogleCastClient(googleCastClient);
	}

	protected void onCreateGoogleCastClient(EZCastOverGoogleCast googleCastClient) {
	}

	@Override
	public void disconnect() {
	
		if (googleCastClient != null) {
			EZCastOverGoogleCast.releaseClient(googleCastClient, connectionManager);			
		}
	}

}