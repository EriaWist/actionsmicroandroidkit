package com.actionsmicro.androidkit.ezcast;

import android.content.Context;

public abstract class ApiBuilder<T> {

	protected DeviceInfo device;
	private ConnectionManager connectionManager;
	private Context context;

	public DeviceInfo getDevice() {
		return device;
	}

	public ApiBuilder<T> setDevice(DeviceInfo device) {
		this.device = device;
		return this;
	}

	public ApiBuilder(DeviceInfo device, Context context) {
		super();
		this.device = device;
		this.setContext(context);
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public ApiBuilder<T> setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		return this;
	}
	public abstract T build();

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}
}