package com.actionsmicro.androidkit.ezcast;

import android.os.Parcelable;

import java.net.InetAddress;

/**
 * This class wraps information of a device.
 * @author James Chen
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
	protected abstract AudioApi createAudioApi(AudioApiBuilder audioApiBuilder);
	protected abstract MediaPlayerApi createMediaPlayerApi(MediaPlayerApiBuilder mediaPlayerApiBuilder);
	@Override
	public int compareTo(DeviceInfo another) {
		int result = getIpAddress().getHostAddress().compareTo(another.getIpAddress().getHostAddress());
		if (result == 0) {
			String myName = getName() != null ? getName(): "";
			String anothersName = another.getName() != null ? another.getName(): "";
			result = myName.compareTo(anothersName);
		}
		return result;
	}	
	public abstract boolean supportMediaFileExtension(String fileExtension);
	/**
	 * To determine whether the device supports ad.
	 * @return Whether the device supports ad.
	 * @since 2.5
	 */
	public abstract boolean supportAd();

	/**
	 * To determine whether the device supports h264streaming.
	 * @return Whether the device supports h264streaming.
	 * @since 2.6
	 */
	public abstract boolean supportH264Streaming();

	/**
	 * To determine whether the device supports image(yuv) to h264.
	 * @return Whether the device supports image to h264.
	 * @since 2.7
	 */
	public abstract boolean supportImageToH264();


	/**
	 * To get device's capability, ex: bluetooth_device/audio_decoders/mediastreaming
	 * @return Device's capbility which wrapped in JSON format
	 * @since 2.9
	 */
	public abstract String getCapability();

	/**
	 * To set deivices's capability by device's response
	 * @param capability
	 * @since 2.9
	 */
	public abstract void setCapability(String capability);

	/**
	 * To determine whether the device supports avsplit.
	 * @return Whether the device supports avsplit.
	 * @since 2.9
	 */
	public abstract boolean supportAVSplit();

}
