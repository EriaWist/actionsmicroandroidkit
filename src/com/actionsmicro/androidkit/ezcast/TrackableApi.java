package com.actionsmicro.androidkit.ezcast;

public abstract class TrackableApi implements Api {

	private EzCastSdk sdk;
	private DeviceInfo device;

	public TrackableApi(ApiBuilder<?> apiBuilder) {
		sdk = apiBuilder.getSdk();
		device = apiBuilder.getDevice();
	}
	@Override
	public void connect() {
		sdk.connectToDevice(device);
	}
	@Override
	public void disconnect() {
		sdk.disconnectFromDevice(device);
	}

}
