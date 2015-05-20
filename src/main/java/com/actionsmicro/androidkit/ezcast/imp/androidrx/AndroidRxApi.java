package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.ConnectionManager;
import com.actionsmicro.androidkit.ezcast.TrackableApi;

public class AndroidRxApi extends TrackableApi implements Api {

	@SuppressWarnings("unused")
	private static final String TAG = "AndroidRxApi";
	private static AndroidRxClientManager androidRxClientManager = new AndroidRxClientManager(); 
	
	private ConnectionManager connectionManager;
	private ApiBuilder<?> apiBuilder;
	protected ApiBuilder<?> getApiBuilder() {
		return apiBuilder;
	}

	private ConnectionManager connectionManagerWrapper;
	private AndroidRxClient androidRxClient;
	protected ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
	public <T> AndroidRxApi(ApiBuilder<T> apiBuilder) {
		super(apiBuilder);
		this.apiBuilder = apiBuilder;
	}

	protected AndroidRxClient getAndroidRxClient() {
		return androidRxClient;
	}
	@Override
	public void connect() {	
		androidRxClient = androidRxClientManager.create(apiBuilder);
		androidRxClient.setTracker(this);
		connectionManagerWrapper = new ConnectionManager() {
			
			@Override
			public void onConnectionFailed(Api api, Exception e) {
				if (apiBuilder.getConnectionManager() != null) {
					apiBuilder.getConnectionManager().onConnectionFailed(AndroidRxApi.this, e);
				}
				
			}
		};
		androidRxClient.addConnectionManager(connectionManagerWrapper);
		super.connect();
	}

	@Override
	public void disconnect() {
		if (androidRxClient != null) {
			androidRxClientManager.release(androidRxClient, apiBuilder);
			androidRxClient.removeConnectionManager(connectionManagerWrapper);
			androidRxClient = null;
		}
		super.disconnect();
	}

	

}
