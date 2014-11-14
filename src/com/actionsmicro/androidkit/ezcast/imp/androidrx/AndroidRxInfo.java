package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import javax.jmdns.ServiceInfo;

import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.androidkit.ezcast.AuthorizationApi;
import com.actionsmicro.androidkit.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.androidkit.ezcast.MessageApi;
import com.actionsmicro.androidkit.ezcast.MessageApiBuilder;
import com.actionsmicro.androidkit.ezcast.imp.bonjour.BonjourDeviceInfo;

public class AndroidRxInfo extends BonjourDeviceInfo {
	
	private String deviceID;

	public AndroidRxInfo(Parcel in) {
		super(in);
		deviceID = in.readString();
	}
	
	public AndroidRxInfo(ServiceInfo newService) {
		super(newService);
		deviceID = newService.getPropertyString("deviceid");
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
	public void writeToParcel(Parcel parcel, int flags) {
		super.writeToParcel(parcel, flags);
		parcel.writeString(deviceID);
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
		return new AndroidRxDisplayApi(displayApiBuilder);
	}

	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		return new AndroidRxMediaPlayerApi(mediaPlayerApiBuilder);
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
		}
		return getPropertyString(key);	
	}
}
