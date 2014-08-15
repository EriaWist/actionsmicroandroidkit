package com.actionsmicro.androidkit.ezcast.imp.airplay;

import android.content.Context;

import com.actionsmicro.airplay.AirPlayClient;
import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.ConnectionManager;


public class AirPlayApi implements Api {

	private Context context;
	private ConnectionManager connectionManager;
	private AirPlayDeviceInfo device;
	private AirPlayClient airPlayClient;

	public <T> AirPlayApi(ApiBuilder<T> apiBuilder) {
		context = apiBuilder.getContext();
		connectionManager = apiBuilder.getConnectionManager();
		device = ((AirPlayDeviceInfo)apiBuilder.getDevice());		
	}
	protected Context getContext() {
		return context;
	}
	protected ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	protected AirPlayDeviceInfo getDevice() {
		return device;
	}
	protected AirPlayClient getAirPlayClient() {
		return airPlayClient;
	}
	@Override
	public void connect() {
		airPlayClient = new AirPlayClient(context, device.getIpAddress());
	}

	@Override
	public void disconnect() {
		if (airPlayClient != null) {
			airPlayClient.close();
		}
		airPlayClient = null;
	}

}
