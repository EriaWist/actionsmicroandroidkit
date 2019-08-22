package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.ConnectionManager;
import com.actionsmicro.androidkit.ezcast.TrackableApi;
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.pigeon.Client;
import com.actionsmicro.pigeon.Client.OnExceptionListener;
import com.actionsmicro.pigeon.MultiRegionsDisplay;
import com.actionsmicro.pigeon.Pigeon;

public class PigeonApi extends TrackableApi implements Api, SplitScreenInterface, OnExceptionListener {

	protected Client pigeonClient;
	protected ConnectionManager connectionManager;
	protected ProjectorInfo projectorInfo;

	public <T> PigeonApi(ApiBuilder<?> apiBuilder) {
		super(apiBuilder);
		projectorInfo = ((PigeonDeviceInfo)apiBuilder.getDevice()).getProjectorInfo();
		connectionManager = apiBuilder.getConnectionManager();
	}

	@Override
	public void connect() {
		if (pigeonClient == null) {
			pigeonClient = Pigeon.createPigeonClient(projectorInfo.getOsVerion(), projectorInfo);
			pigeonClient.addOnExceptionListener(this);
			this.onPigeonClientCreated(pigeonClient);
			super.connect();
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
			super.disconnect();
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

	@Override
	public int getPosition() {
		if (pigeonClient instanceof MultiRegionsDisplay)
			return ((MultiRegionsDisplay) pigeonClient).getPosition();
		return 0;
	}

	@Override
	public int getSplitCount() {
		if (pigeonClient instanceof MultiRegionsDisplay)
			return ((MultiRegionsDisplay) pigeonClient).getNumberOfRegions();
		return 0;
	}

}