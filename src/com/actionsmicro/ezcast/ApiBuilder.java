package com.actionsmicro.ezcast;

public abstract class ApiBuilder<T> {

	protected DeviceInfo device;
	private ConnectionManager connectionManager;

	public DeviceInfo getDevice() {
		return device;
	}

	public ApiBuilder<T> setDevice(DeviceInfo device) {
		this.device = device;
		return this;
	}

	public ApiBuilder(DeviceInfo device) {
		super();
		this.device = device;
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public ApiBuilder<T> setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		return this;
	}
	public abstract T build();
}