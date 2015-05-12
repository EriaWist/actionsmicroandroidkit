package com.actionsmicro.androidkit.ezcast.imp.androidp2prx;

import java.net.InetAddress;

import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.androidkit.ezcast.AudioApi;
import com.actionsmicro.androidkit.ezcast.AudioApiBuilder;
import com.actionsmicro.androidkit.ezcast.AuthorizationApi;
import com.actionsmicro.androidkit.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.androidkit.ezcast.EzCastSdk;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.androidkit.ezcast.MessageApi;
import com.actionsmicro.androidkit.ezcast.MessageApiBuilder;
import com.actionsmicro.androidkit.ezcast.imp.bonjour.BonjourDeviceInfo;
import com.actionsmicro.p2p.P2PWebApi;

public class AndroidP2PRxInfo extends DeviceInfo {
	
	private String deviceID;
	
	private static final String TAG="AndroidP2PRxInfo";
	
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

	private int port;
	private InetAddress address;
	private String name;
	
	public AndroidP2PRxInfo(Parcel in) {
		deviceID = in.readString();
		port = in.readInt();
		address = (InetAddress)in.readSerializable();
		name = in.readString();
		
	}
	
	public AndroidP2PRxInfo(String deviceUUID) {
		deviceID = deviceUUID;
		port = 0;
		address = P2PWebApi.getLocalAddr();
		name = "P2P_"+deviceUUID;
		
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public String getVendor() {
		return "EZCast";
	}
	
	@Override
	public InetAddress getIpAddress() {
		return address;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public String getdeviceid() {
		return deviceID;
	}
	
	public int getPort() {
		if (port==0) {
			String type = "jsonrpc";
			String hostport = EzCastSdk.getp2pwebapi().QueryHostUUIDPort(deviceID, type);
			int nHostPort=Integer.parseInt(hostport);
			port=nHostPort;
		}
		return port;
	}
	
	//public AndroidP2PRxInfo(ServiceInfo newService) {
	//	super(newService);
	//	deviceID = newService.getPropertyString("deviceid");
	//}
	
	public static final Parcelable.Creator<AndroidP2PRxInfo> CREATOR = new Parcelable.Creator<AndroidP2PRxInfo>() {
		public AndroidP2PRxInfo createFromParcel(Parcel in) {
			return new AndroidP2PRxInfo(in);
		}

		public AndroidP2PRxInfo[] newArray(int size) {
			return new AndroidP2PRxInfo[size];
		}
	};
		
	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		//super.writeToParcel(parcel, flags);
		parcel.writeString(deviceID);
		parcel.writeInt(port);
		parcel.writeSerializable(address);
		parcel.writeString(name);
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
	protected MessageApi createMessageApi(MessageApiBuilder messageApiBuilder) {
		return null;
	}

	@Override
	protected AuthorizationApi createAuthorizationApi(
			AuthorizationApiBuilder authorizationApiBuilder) {
		return null;
	}

	@Override
	protected DisplayApi createDisplayApi(DisplayApiBuilder displayApiBuilder) {		
		return new AndroidP2PRxDisplayApi(displayApiBuilder);
	}

	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		return new AndroidP2PRxMediaPlayerApi(mediaPlayerApiBuilder);
	}
	
	@Override
	public boolean supportMediaFileExtension(String fileExtension) {
		// TODO Auto-generated method stub
		return true;
	}	
	
	@Override
	public String getParameter(String key) {
		if (key.equalsIgnoreCase("ezcast.service.android")) {
			return Long.toHexString(SERVICE_PHOTO | SERVICE_CAMERA | SERVICE_MUSIC | SERVICE_VIDEO | SERVICE_DOCUMENT | SERVICE_WEB | SERVICE_CLOUD_VIDEO | SERVICE_CLOUD_STORAGE | SERVICE_LIVE | SERVICE_EZCAST | SERVICE_COMMENT | SERVICE_UPDATE | SERVICE_SOCIAL);   
		} else if (key.equals("deviceid")) {
			return deviceID;
		} else if (key.equals("port")) {
			return String.valueOf(port);
		}
		return "";	
	}
	
	@Override
    protected AudioApi createAudioApi(AudioApiBuilder displayApiBuilder) {
        // TODO Auto-generated method stub
        return null;
    }
}
