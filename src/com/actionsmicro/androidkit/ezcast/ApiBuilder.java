package com.actionsmicro.androidkit.ezcast;

import android.content.Context;


/**
 * Base class of EZCast API builder.
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */
public abstract class ApiBuilder<API> {

	protected DeviceInfo device;
	private ConnectionManager connectionManager;
	private Context context;

	public ApiBuilder(DeviceInfo device, Context context) {
		super();
		this.device = device;
		this.context = context;
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	/**
	 * Set the {@link ConnectionManager} to handle connection events of the API.
	 * @param connectionManager
	 * @return ApiBuilder itself for chaining.
	 * @since 2.0
	 */

	public ApiBuilder<API> setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		return this;
	}
	/**
	 * Create an {@link Api} object. 
	 * @return The {@link Api} object.
	 * @since 2.0
	 */
	public abstract API build();

	public Context getContext() {
		return context;
	}
	public DeviceInfo getDevice() {
		return device;
	}

}