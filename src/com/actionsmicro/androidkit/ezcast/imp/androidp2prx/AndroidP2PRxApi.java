package com.actionsmicro.androidkit.ezcast.imp.androidp2prx;

import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.ConnectionManager;
import com.actionsmicro.androidkit.ezcast.TrackableApi;

public class AndroidP2PRxApi extends TrackableApi implements Api {

	@SuppressWarnings("unused")
	private static final String TAG = "AndroidRxApi";
	private static AndroidP2PRxClientManager androidP2PRxClientManager = new AndroidP2PRxClientManager(); 
	
	private ConnectionManager connectionManager;
	private ApiBuilder<?> apiBuilder;
	protected ApiBuilder<?> getApiBuilder() {
		return apiBuilder;
	}

	private ConnectionManager connectionManagerWrapper;
	private AndroidP2PRxClient androidP2PRxClient;
	protected ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
	public <T> AndroidP2PRxApi(ApiBuilder<T> apiBuilder) {
		super(apiBuilder);
		this.apiBuilder = apiBuilder;
	}

	protected AndroidP2PRxClient getAndroidP2PRxClient() {
		return androidP2PRxClient;
	}
	@Override
	public void connect() {	
		androidP2PRxClient = androidP2PRxClientManager.create(apiBuilder);
		androidP2PRxClient.setTracker(this);
		connectionManagerWrapper = new ConnectionManager() {
			
			@Override
			public void onConnectionFailed(Api api, Exception e) {
				if (apiBuilder.getConnectionManager() != null) {
					apiBuilder.getConnectionManager().onConnectionFailed(AndroidP2PRxApi.this, e);
				}
				
			}
		};
		androidP2PRxClient.addConnectionManager(connectionManagerWrapper);
		super.connect();
	}

	@Override
	public void disconnect() {
		if (androidP2PRxClient != null) {
			androidP2PRxClientManager.release(androidP2PRxClient, apiBuilder);
			androidP2PRxClient.removeConnectionManager(connectionManagerWrapper);
			androidP2PRxClient = null;
		}
		super.disconnect();
	}

	

}
