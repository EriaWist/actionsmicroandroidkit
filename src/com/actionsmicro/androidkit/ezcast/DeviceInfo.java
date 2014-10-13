package com.actionsmicro.androidkit.ezcast;

import java.net.InetAddress;

import android.os.Parcelable;

/**
 * This class wraps information of a device.
 * @author jamchen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */

public abstract class DeviceInfo implements Parcelable, Comparable<DeviceInfo> {
	/**
	 * The IP address of the device.
	 * @return The IP address of the device.
	 * @since 2.0
	 */
	public abstract InetAddress getIpAddress();
	/**
	 * To determine whether the device supports HTTP streaming.
	 * @return Whether the device supports HTTP streaming.
	 * @since 2.0
	 */
	public abstract boolean supportsHttpStreaming();
	/**
	 * To determine whether the device supports split screen.
	 * @return Whether the device supports split screen.
	 * @since 2.0
	 */
	public abstract boolean supportsSplitScreen();
	/**
	 * To determine whether the device supports remote control.
	 * @return Whether the device supports remote control.
	 * @since 2.0
	 */
	public abstract boolean supportsRemoteControl();
	public abstract boolean supportsDisplay();
	public abstract String getVendor();
	public abstract String getName();
	public abstract String getParameter(String key);
	protected abstract MessageApi createMessageApi(MessageApiBuilder messageApiBuilder);
	protected abstract AuthorizationApi createAuthorizationApi(AuthorizationApiBuilder authorizationApiBuilder);
	protected abstract DisplayApi createDisplayApi(DisplayApiBuilder displayApiBuilder);
	protected abstract MediaPlayerApi createMediaPlayerApi(MediaPlayerApiBuilder mediaPlayerApiBuilder);
	@Override
	public int compareTo(DeviceInfo another) {
		return getIpAddress().getHostAddress().compareTo(another.getIpAddress().getHostAddress());
	}	
	public abstract boolean supportMediaFileExtension(String fileExtension);
}
