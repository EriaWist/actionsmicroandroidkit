package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.media.MediaRouter.RouteInfo;

import com.actionsmicro.androidkit.ezcast.AudioApi;
import com.actionsmicro.androidkit.ezcast.AudioApiBuilder;
import com.actionsmicro.androidkit.ezcast.AuthorizationApi;
import com.actionsmicro.androidkit.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.androidkit.ezcast.MessageApi;
import com.actionsmicro.androidkit.ezcast.MessageApiBuilder;
import com.google.android.gms.cast.CastDevice;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GoogleCastDeviceInfo extends DeviceInfo {
	private CastDevice castDevice;

	public CastDevice getCastDevice() {
		return castDevice;
	}
	public GoogleCastDeviceInfo(CastDevice castDevice) {
		this.castDevice = castDevice;
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
	private static final long SERVICE_SOCIAL			= 0x100000;
	
	@Override
	public String getParameter(String key) {
		if (key.equalsIgnoreCase("ezcast.service.android")) {
			return Long.toHexString(SERVICE_PHOTO | SERVICE_CAMERA | SERVICE_MUSIC | SERVICE_VIDEO | SERVICE_DOCUMENT | SERVICE_WEB | SERVICE_CLOUD_VIDEO | SERVICE_CLOUD_STORAGE | SERVICE_LIVE | SERVICE_EZCAST | SERVICE_COMMENT | SERVICE_UPDATE | SERVICE_SOCIAL);   
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
		return null;
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
	private static List<String> supportedFileExtension = Arrays.asList(new String[]{"mp4","mp3","mov"});
	@Override
	public boolean supportMediaFileExtension(String fileExtension) {
		return supportedFileExtension.contains(fileExtension.toLowerCase(Locale.US));
	}

	@Override
	public boolean supportAd() {
		return true;
	}

	@Override
	public boolean supportH264Streaming() {
		return true;
	}

	@Override
	public boolean supportImageToH264() {
		return true;
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
    protected AudioApi createAudioApi(AudioApiBuilder displayApiBuilder) {
        // TODO Auto-generated method stub
        return null;
    }

}
