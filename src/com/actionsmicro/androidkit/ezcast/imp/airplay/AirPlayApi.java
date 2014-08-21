package com.actionsmicro.androidkit.ezcast.imp.airplay;

import com.actionsmicro.airplay.AirPlayClient;
import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;


public class AirPlayApi implements Api {

	private AirPlayClient airPlayClient;
	private ApiBuilder<?> apiBuilder;
	private static AirPlayClientManager airPlayClientManager = new AirPlayClientManager(); 
	public <T> AirPlayApi(ApiBuilder<T> apiBuilder) {
		this.apiBuilder = apiBuilder;
	}
	protected AirPlayClient getAirPlayClient() {
		return airPlayClient;
	}
	@Override
	public void connect() {
		airPlayClient = airPlayClientManager.create(apiBuilder);
	}

	@Override
	public void disconnect() {
		airPlayClientManager.release(airPlayClient, apiBuilder);
		airPlayClient = null;
	}

}
