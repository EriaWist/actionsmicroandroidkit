package com.actionsmicro.ezcast;

import java.net.InetAddress;

import android.os.Parcelable;

public abstract class DeviceInfo implements Parcelable {
	public abstract InetAddress getIpAddress();
	public abstract boolean supportsHttpStreaming();
	public abstract boolean supportsSplitScreen();
	public abstract boolean supportsRemoteControl();
	public abstract boolean supportsDisplay();
	public abstract String getVendor();
	public abstract String getName();
	public abstract String getParameter(String key);
	protected abstract MessageApi createMessageApi(MessageApiBuilder messageApiBuilder);
	protected abstract AuthorizationApi createAuthorizationApi(AuthorizationApiBuilder authorizationApiBuilder);
	protected abstract DisplayApi createDisplayApi(DisplayApiBuilder displayApiBuilder);
	protected abstract MediaPlayerApi createMediaPlayerApi(MediaPlayerApiBuilder mediaPlayerApiBuilder);
}
