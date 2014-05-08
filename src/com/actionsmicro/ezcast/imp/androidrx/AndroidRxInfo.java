package com.actionsmicro.ezcast.imp.androidrx;

import java.net.InetAddress;

import javax.jmdns.ServiceInfo;

import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.ezcast.AuthorizationApi;
import com.actionsmicro.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.ezcast.DeviceInfo;
import com.actionsmicro.ezcast.DisplayApi;
import com.actionsmicro.ezcast.DisplayApiBuilder;
import com.actionsmicro.ezcast.MediaPlayerApi;
import com.actionsmicro.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.ezcast.MessageApi;
import com.actionsmicro.ezcast.MessageApiBuilder;

public class AndroidRxInfo extends DeviceInfo {
	private int port;
	private InetAddress address;
	private String name;
//	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//	public AndroidRxInfo(NsdServiceInfo serviceInfo) {
//		this.port = serviceInfo.getPort();
//		this.address = serviceInfo.getHost();
//		this.name = serviceInfo.getServiceName();
//	}
	public AndroidRxInfo(Parcel in) {
		port = in.readInt();
		address = (InetAddress)in.readSerializable();
		name = in.readString();
	}
	public AndroidRxInfo(ServiceInfo newService) {
		this.port = newService.getPort();
		this.address = newService.getInet4Address();
		this.name = newService.getName();
	}
	public static final Parcelable.Creator<AndroidRxInfo> CREATOR = new Parcelable.Creator<AndroidRxInfo>() {
		public AndroidRxInfo createFromParcel(Parcel in) {
			return new AndroidRxInfo(in);
		}

		public AndroidRxInfo[] newArray(int size) {
			return new AndroidRxInfo[size];
		}
	};
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(port);
		parcel.writeSerializable(address);
		parcel.writeString(name);
	}

	@Override
	public InetAddress getIpAddress() {
		return address;
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
		return "EZCast";
	}

	@Override
	public String getName() {
		return name;
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
	private static final long SERVICE_SOCIAL			= 0x100000;
	
	@Override
	public String getParameter(String key) {
		if (key.equalsIgnoreCase("ezcast.service.android")) {
			return Long.toHexString(SERVICE_PHOTO | SERVICE_CAMERA | SERVICE_MUSIC | SERVICE_VIDEO | SERVICE_DOCUMENT | SERVICE_WEB | SERVICE_CLOUD_VIDEO | SERVICE_CLOUD_STORAGE | SERVICE_LIVE | SERVICE_EZCAST | SERVICE_COMMENT | SERVICE_UPDATE | SERVICE_SOCIAL);   
		}
		return null;	
	}

	@Override
	protected MessageApi createMessageApi(MessageApiBuilder messageApiBuilder) {
		return new AndroidRxMessageApi(messageApiBuilder);
	}

	@Override
	protected AuthorizationApi createAuthorizationApi(
			AuthorizationApiBuilder authorizationApiBuilder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected DisplayApi createDisplayApi(DisplayApiBuilder displayApiBuilder) {		
		return new AndroidRxDisplayApi(displayApiBuilder);
	}

	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		return new AndroidRxMediaPlayerApi(mediaPlayerApiBuilder);
	}
	public int getPort() {
		return this.port;		
	}
	@Override
	public boolean supportMediaFileExtension(String fileExtension) {
		// TODO Auto-generated method stub
		return true;
	}

}
