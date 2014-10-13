package com.actionsmicro.androidkit.ezcast;
/**
 * Connection related callback.
 * @author jamchen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */
public interface ConnectionManager {
	/**
	 * Called when connection between API object and the device has failed.
	 * @param api The API object.
	 * @param e The reason why the connection was failed.
	 * @since 2.0
	 */
	public void onConnectionFailed(Api api, Exception e);
}
