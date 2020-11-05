package com.actionsmicro.androidkit.ezcast.imp.airplay;

import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.androidkit.ezcast.AudioApi;
import com.actionsmicro.androidkit.ezcast.AudioApiBuilder;
import com.actionsmicro.androidkit.ezcast.AuthorizationApi;
import com.actionsmicro.androidkit.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.androidkit.ezcast.MessageApi;
import com.actionsmicro.androidkit.ezcast.MessageApiBuilder;
import com.actionsmicro.androidkit.ezcast.imp.bonjour.BonjourDeviceInfo;

import javax.jmdns.ServiceInfo;

public class AirPlayDeviceInfo extends BonjourDeviceInfo {

	private String deviceID;
	private String rmodel;

	public AirPlayDeviceInfo(Parcel in) {
		super(in);
		deviceID = in.readString();
		rmodel = in.readString();
	}
	
	public AirPlayDeviceInfo(ServiceInfo newService) {
		super(newService);
		deviceID = newService.getPropertyString("deviceid");
		rmodel   = newService.getPropertyString("rmodel");
	}
		
	public static final Parcelable.Creator<AirPlayDeviceInfo> CREATOR = new Parcelable.Creator<AirPlayDeviceInfo>() {
		public AirPlayDeviceInfo createFromParcel(Parcel in) {
			return new AirPlayDeviceInfo(in);
		}

		public AirPlayDeviceInfo[] newArray(int size) {
			return new AirPlayDeviceInfo[size];
		}
	};
	
	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		super.writeToParcel(parcel, flags);
		parcel.writeString(deviceID);
		parcel.writeString(rmodel);
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
		return new AirPlayDisplayApi(displayApiBuilder);
	}

	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		return new AirPlayMediaPlayerApi(mediaPlayerApiBuilder);
	}

	@Override
	public boolean supportMediaFileExtension(String fileExtension) {
		return true;
	}

	@Override
	public boolean supportAd() {
		return true;
	}

	@Override
	public boolean supportH264Streaming() {
		return false;
	}

	@Override
	public boolean supportImageToH264() {
		return false;
	}

	@Override
	public String getCapability() {
		return null;
	}

	@Override
	public void setCapability(String capability) {

	}

	@Override
	public boolean supportAVSplit() {
		return false;
	}

	@Override
	public boolean supportAACELD() {
		return false;
	}

	@Override
	public String getParameter(String key) {
		if (key.equalsIgnoreCase("ezcast.service.android")) {
			return Long.toHexString(SERVICE_VIDEO | SERVICE_WEB | SERVICE_CLOUD_VIDEO | SERVICE_CLOUD_STORAGE | SERVICE_LIVE | SERVICE_EZCAST | SERVICE_COMMENT | SERVICE_UPDATE | SERVICE_SOCIAL);   
		} else if (key.equals("deviceid")) {
			return deviceID;
		} else if(key.equals("rmodel")){
			return rmodel;
		}
		return getPropertyString(key);
	}

    @Override
    protected AudioApi createAudioApi(AudioApiBuilder displayApiBuilder) {
        // TODO Auto-generated method stub
        return null;
    }
}
