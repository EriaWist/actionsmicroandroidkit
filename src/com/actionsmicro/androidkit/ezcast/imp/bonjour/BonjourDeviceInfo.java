package com.actionsmicro.androidkit.ezcast.imp.bonjour;

import java.net.InetAddress;

import javax.jmdns.ServiceInfo;

import android.os.Parcel;

import com.actionsmicro.androidkit.ezcast.DeviceInfo;

public abstract class BonjourDeviceInfo extends DeviceInfo {
	private int port;
	private InetAddress address;
	private String name;
	
	public BonjourDeviceInfo(Parcel in) {
		port = in.readInt();
		address = (InetAddress)in.readSerializable();
		name = in.readString();
	}
	
	@SuppressWarnings("deprecation")
	public BonjourDeviceInfo(ServiceInfo newService) {
		this.port = newService.getPort();
		this.address = newService.getInet4Address();
		this.name = newService.getName();
	}
	
	public static final BonjourDeviceInfo.Creator<? extends BonjourDeviceInfo> DEVICE_CREATOR = null;
	
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
	public String getVendor() {
		return "EZCast";
	}

	@Override
	public String getName() {
		return name;
	}


	public BonjourDeviceInfo() {
		super();
	}

	public int getPort() {
		return this.port;		
	}

	protected static final long SERVICE_PHOTO = 0x01;
	protected static final long SERVICE_CAMERA = 0x02;
	protected static final long SERVICE_MUSIC = 0x04;
	protected static final long SERVICE_VIDEO = 0x08;
	protected static final long SERVICE_DLNA = 0x10;
	protected static final long SERVICE_EZMIRROR = 0x20;
	protected static final long SERVICE_DOCUMENT = 0x40;
	protected static final long SERVICE_WEB = 0x80;
	protected static final long SERVICE_SETTING = 0x100;
	protected static final long SERVICE_EZAIR = 0x200;
	protected static final long SERVICE_CLOUD_VIDEO = 0x400;
	protected static final long SERVICE_MAP = 0x800;
	protected static final long SERVICE_CLOUD_STORAGE = 0x1000;
	protected static final long SERVICE_LIVE = 0x2000;
	protected static final long SERVICE_SPLIT_SCREEN = 0x4000;
	protected static final long SERVICE_EZCAST = 0x8000;
	protected static final long SERVICE_COMMENT = 0x10000;
	protected static final long SERVICE_UPDATE = 0x20000;
	protected static final long SERVICE_NEWS = 0x40000;
	protected static final long SERVICE_MESSAGES = 0x80000;
	protected static final long SERVICE_SOCIAL = 0x100000;
}