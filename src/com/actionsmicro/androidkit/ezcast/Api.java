package com.actionsmicro.androidkit.ezcast;



/**
 * Base interface of EZCast API.
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */
public interface Api {
	/**
	 * Connect the API.
	 * Remember to call {@link #disconnect()} when you no longer need the API.
	 * 
	 * @see #disconnect()
	 * @since 2.0
	 */
	public void connect();
	/**
	 * Disconnect the API.
	 * @see #connect()
	 * @since 2.0
	 */
	public void disconnect();
}
