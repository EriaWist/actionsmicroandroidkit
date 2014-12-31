package com.actionsmicro.androidkit.ezcast.imp.dlna;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;

import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.androidkit.ezcast.AuthorizationApi;
import com.actionsmicro.androidkit.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.androidkit.ezcast.MessageApi;
import com.actionsmicro.androidkit.ezcast.MessageApiBuilder;
import com.actionsmicro.utils.Log;

public class DlnaDeviceInfo extends DeviceInfo {

	public static final Parcelable.Creator<DlnaDeviceInfo> CREATOR = new Parcelable.Creator<DlnaDeviceInfo>() {
		public DlnaDeviceInfo createFromParcel(Parcel in) {
			return new DlnaDeviceInfo(in);
		}

		public DlnaDeviceInfo[] newArray(int size) {
			return new DlnaDeviceInfo[size];
		}
	};
	private Device device;
	public Device getDevice() {
		return device;
	}

	public DlnaDeviceInfo(Device device) {
		this.device = device;
	}

	public DlnaDeviceInfo(Parcel in) {
		String udn = in.readString();
		this.device = UpnpService.getUpnpService().getDeviceById(udn);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(device.getIdentity().getUdn().getIdentifierString());
	}

	@Override
	public InetAddress getIpAddress() {
		DeviceIdentity di = device.getIdentity();
		if (di instanceof RemoteDeviceIdentity) {
			RemoteDeviceIdentity rdi = (RemoteDeviceIdentity)di;
			try {
				return InetAddress.getByName(rdi.getDescriptorURL().getHost());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
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
		return false;
	}

	@Override
	public String getVendor() {
		return device.getDetails().getManufacturerDetails().getManufacturer();
	}

	@Override
	public String getName() {
		return device.getDetails().getFriendlyName();
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
	private static final String TAG = "DlnaDeviceInfo";
	
	@Override
	public String getParameter(String key) {
		if (key.equalsIgnoreCase("ezcast.service.android")) {
			return Long.toHexString(SERVICE_PHOTO | SERVICE_CAMERA | SERVICE_MUSIC | SERVICE_VIDEO | SERVICE_DOCUMENT | SERVICE_WEB | SERVICE_CLOUD_VIDEO | SERVICE_CLOUD_STORAGE | SERVICE_LIVE | SERVICE_EZCAST | SERVICE_COMMENT | SERVICE_UPDATE | SERVICE_SOCIAL);   
		}
		if (key.equalsIgnoreCase("deviceid")) {
			return device.getIdentity().getUdn().getIdentifierString();
		}
		if (key.equalsIgnoreCase("srcvers")) {
			return device.getVersion().toString();
		}
		return null;
	}

	@Override
	protected MessageApi createMessageApi(MessageApiBuilder messageApiBuilder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected AuthorizationApi createAuthorizationApi(
			AuthorizationApiBuilder authorizationApiBuilder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected DisplayApi createDisplayApi(DisplayApiBuilder displayApiBuilder) {
//		return new DlnaDisplayApi(displayApiBuilder);
		return null;
	}

	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		return new DlnaMediaPlayerApi(mediaPlayerApiBuilder);
	}

	@Override
	public boolean supportMediaFileExtension(String fileExtension) {
		return true;
	}

	public String getManufacturer() {
		try {
			String manufacturer = device.getDetails().getManufacturerDetails().getManufacturer();
			return manufacturer;
		} catch(Throwable e) {
			Log.e(TAG, "getManufacturer", e);
		}
		return null;
	}

	public String getModel() {
		try {
			String model = device.getDetails().getModelDetails().getModelName();
			return model;
		} catch(Throwable e) {
			Log.e(TAG, "getManufacturer", e);
		}
		return null;		
	}

}
