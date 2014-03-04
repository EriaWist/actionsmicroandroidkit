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
	private static final long SERVICE_PHOTO			=	0x01;
	private static final long SERVICE_CAMERA			=   0x02;
	private static final long SERVICE_MUSIC			=   0x04;
	private static final long SERVICE_VIDEO			=   0x08;
	private static final long SERVICE_DLNA 			=   0x10;
	private static final long SERVICE_EZMIRROR		=   0x20;
	private static final long SERVICE_DOCUMENT		=   0x40;
	private static final long SERVICE_WEB			=   0x80;
	private static final long SERVICE_SETTING		=  0x100;
	private static final long SERVICE_EZAIR			=  0x200;
	private static final long SERVICE_CLOUD_VIDEO 	=  0x400;
	private static final long SERVICE_MAP			=  0x800;
	private static final long SERVICE_CLOUD_STORAGE	= 0x1000;
	private static final long SERVICE_LIVE			= 0x2000;
	private static final long SERVICE_SPLIT_SCREEN	= 0x4000;
	private static final long SERVICE_EZCAST			= 0x8000;
	private static final long SERVICE_COMMENT		= 0x10000;
	private static final long SERVICE_UPDATE			= 0x20000;
	private static final long SERVICE_NEWS			= 0x40000;
	private static final long SERVICE_MESSAGES		= 0x80000;
	
	@Override
	public String getParameter(String key) {
		if (key.equalsIgnoreCase("ezcast.service.android")) {
			return Long.toHexString(SERVICE_PHOTO | SERVICE_CAMERA | SERVICE_MUSIC | SERVICE_VIDEO | SERVICE_DOCUMENT | SERVICE_WEB | SERVICE_CLOUD_VIDEO | SERVICE_CLOUD_STORAGE | SERVICE_LIVE | SERVICE_EZCAST | SERVICE_COMMENT | SERVICE_UPDATE);   
		}
		if (key.equalsIgnoreCase("deviceid")) {
			return castDevice.getDeviceId();
		}
		if (key.equalsIgnoreCase("srcvers")) {
			return castDevice.getDeviceVersion();
		}
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
