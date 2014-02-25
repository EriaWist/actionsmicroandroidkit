package com.actionsmicro.ezcast.imp.googlecast;

import java.net.InetAddress;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.media.MediaRouter.RouteInfo;

import com.actionsmicro.ezcast.AuthorizationApi;
import com.actionsmicro.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.ezcast.DeviceInfo;
import com.actionsmicro.ezcast.DisplayApi;
import com.actionsmicro.ezcast.DisplayApiBuilder;
import com.actionsmicro.ezcast.MediaPlayerApi;
import com.actionsmicro.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.ezcast.MessageApi;
import com.actionsmicro.ezcast.MessageApiBuilder;
import com.google.android.gms.cast.CastDevice;

public class GoogleCastDeviceInfo extends DeviceInfo {
	private CastDevice castDevice;

	public CastDevice getCastDevice() {
		return castDevice;
	}
	public GoogleCastDeviceInfo(RouteInfo routeInfo) {
		castDevice = CastDevice.getFromBundle(routeInfo.getExtras());
	}
	public GoogleCastDeviceInfo(Parcel in) {
		castDevice = CastDevice.CREATOR.createFromParcel(in);
	}
	public static final Parcelable.Creator<GoogleCastDeviceInfo> CREATOR = new Parcelable.Creator<GoogleCastDeviceInfo>() {
		public GoogleCastDeviceInfo createFromParcel(Parcel in) {
			return new GoogleCastDeviceInfo(in);
		}

		public GoogleCastDeviceInfo[] newArray(int size) {
			return new GoogleCastDeviceInfo[size];
		}
	};
	@Override
	public int describeContents() {
		return castDevice.describeContents();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		castDevice.writeToParcel(dest, flags);
	}

	@Override
	public InetAddress getIpAddress() {
		return castDevice.getIpAddress();
	}

	@Override
	public boolean supportsHttpStreaming() {
		return true;
	}

	@Override
	public boolean supportsSplitScreen() {
		return false;
	}

	@Override
	public boolean supportsRemoteControl() {
		return false;
	}

	@Override
	public boolean supportsDisplay() {
		return true;
	}

	@Override
	public String getVendor() {
		return "Google";
	}

	@Override
	public String getName() {
		return castDevice.getFriendlyName();
	}

	@Override
	public String getParameter(String key) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	protected MessageApi createMessageApi(MessageApiBuilder messageApiBuilder) {
		return new GoogleCastMessageApi(messageApiBuilder);
	}
	@Override
	protected AuthorizationApi createAuthorizationApi(
			AuthorizationApiBuilder authorizationApiBuilder) {
		return new GoogleCastAuthorizationApi(authorizationApiBuilder);
	}
	@Override
	protected DisplayApi createDisplayApi(DisplayApiBuilder displayApiBuilder) {
		return new GoogleCastDisplayApi(displayApiBuilder);
	}
	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		return new GoogleCastMediaPlayerApi(mediaPlayerApiBuilder);
	}

}
