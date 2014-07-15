package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.ConnectionManager;
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.pigeon.Client;
import com.actionsmicro.pigeon.Client.OnExceptionListener;
import com.actionsmicro.pigeon.Pigeon;

public class PigeonApi implements Api, OnExceptionListener {

	protected Client pigeonClient;
	protected ConnectionManager connectionManager;
	protected ProjectorInfo projectorInfo;

	public <T> PigeonApi(ApiBuilder<T> apiBuilder) {
		super();
		projectorInfo = ((PigeonDeviceInfo)apiBuilder.getDevice()).getProjectorInfo();
		connectionManager = apiBuilder.getConnectionManager();
	}

	@Override
	public void connect() {
		if (pigeonClient == null) {
			pigeonClient = Pigeon.createPigeonClient(projectorInfo.getOsVerion(), projectorInfo.getAddress().getHostAddress(), Falcon.EZ_WIFI_DISPLAY_PORT_NUMBER);
			pigeonClient.addOnExceptionListener(this);
			this.onPigeonClientCreated(pigeonClient);
		}
	}

	protected void onPigeonClientCreated(Client pigeonClient) {
		
	}

	@Override
	public void disconnect() {
		if (pigeonClient != null) {
			this.onPigeonClientReleased(pigeonClient);
			pigeonClient.removeOnExceptionListener(this);
			Pigeon.releasePigeonClient(pigeonClient);
			pigeonClient = null;
		}
	}

	protected void onPigeonClientReleased(Client pigeonClient) {
		
	}

	@Override
	public void onException(Client client, Exception e) {
		if (connectionManager != null) {
			connectionManager.onConnectionFailed(this, e);
		}
	}

}