package com.actionsmicro.androidkit.ezcast.imp.airplay;

import com.actionsmicro.airplay.AirPlayClient;
import com.actionsmicro.airplay.AirPlayClient.ConnectionManager;
import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.TrackableApi;


public class AirPlayApi extends TrackableApi implements Api {

	private AirPlayClient airPlayClient;
	private ApiBuilder<?> apiBuilder;
	private ConnectionManager connectionManagerWrapper;
	private static AirPlayClientManager airPlayClientManager = new AirPlayClientManager(); 
	public <T> AirPlayApi(ApiBuilder<T> apiBuilder) {
		super(apiBuilder);
		this.apiBuilder = apiBuilder;
	}
	protected AirPlayClient getAirPlayClient() {
		return airPlayClient;
	}
	@Override
	public void connect() {
		airPlayClient = airPlayClientManager.create(apiBuilder);
		connectionManagerWrapper = new AirPlayClient.ConnectionManager() {
			
			@Override
			public void onConnectionFailed(Exception e) {
				if (apiBuilder.getConnectionManager() != null) {
					apiBuilder.getConnectionManager().onConnectionFailed(AirPlayApi.this, e);
				}
			}
		};
		airPlayClient.addConnectionManager(connectionManagerWrapper);
		super.connect();
	}

	@Override
	public void disconnect() {
		airPlayClientManager.release(airPlayClient, apiBuilder);
		airPlayClient.removeConnectionManager(connectionManagerWrapper);
		airPlayClient = null;
		super.disconnect();
	}

}
