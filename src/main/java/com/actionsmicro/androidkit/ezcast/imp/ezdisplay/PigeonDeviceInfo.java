package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import android.os.Parcel;
import android.os.Parcelable;

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
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.pigeon.Client;
import com.actionsmicro.pigeon.Pigeon;

import java.net.InetAddress;

public class PigeonDeviceInfo extends DeviceInfo {

	private ProjectorInfo projectorInfo;

	public ProjectorInfo getProjectorInfo() {
		return projectorInfo;
	}
	public PigeonDeviceInfo(ProjectorInfo projectorInfo) {
		this.projectorInfo = projectorInfo;
	}
	public PigeonDeviceInfo(Parcel in) {
		this.projectorInfo = ProjectorInfo.CREATOR.createFromParcel(in);
	}
	public static final Parcelable.Creator<PigeonDeviceInfo> CREATOR = new Parcelable.Creator<PigeonDeviceInfo>() {
		public PigeonDeviceInfo createFromParcel(Parcel in) {
			return new PigeonDeviceInfo(in);
		}

		public PigeonDeviceInfo[] newArray(int size) {
			return new PigeonDeviceInfo[size];
		}
	};
	@Override
	public int describeContents() {
		return projectorInfo.describeContents();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		projectorInfo.writeToParcel(dest, flags);
	}
	@Override
	public boolean supportsHttpStreaming() {
		return projectorInfo.supportsHttpStreaming();
	}
	@Override
	public InetAddress getIpAddress() {
		return projectorInfo.getAddress();
	}
	@Override
	public boolean supportsSplitScreen() {
		return projectorInfo.supportsSplitScreen();
	}
	@Override
	public boolean supportsRemoteControl() {
		return projectorInfo.isRemoteControlEnabled();
	}
	@Override
	public String getVendor() {
		return projectorInfo.getVendor();
	}
	@Override
	public String getName() {
		return projectorInfo.getName();
	}
	@Override
	public String getParameter(String key) {
		return projectorInfo.getParameter(key);
	}
	@Override
	public boolean supportsDisplay() {
		return projectorInfo.getWifiDisplayPortNumber() != 0;
	}
	@Override
	protected MessageApi createMessageApi(MessageApiBuilder messageApiBuilder) {
		return new PigeonMessageApi(messageApiBuilder);
	}
	@Override
	protected AuthorizationApi createAuthorizationApi(
			AuthorizationApiBuilder authorizationApiBuilder) {
		return new PigeonAuthorizationApi(authorizationApiBuilder);
	}
	@Override
	protected DisplayApi createDisplayApi(DisplayApiBuilder displayApiBuilder) {
		if (!isAuthorized()) {
			return null;
		}
		String type = getProjectorInfo().getParameter("type");
		if (type != null && type.equals("music")) {
			return null;
		}
		return new PigeonDisplayApi(displayApiBuilder);
	}
	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		if (!isAuthorized()) {
			return null;
		}
		return new PigeonMediaPlayerApi(mediaPlayerApiBuilder);
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
	protected AudioApi createAudioApi(AudioApiBuilder audioApiBuilder) {
		return new PigeonAudioApi(audioApiBuilder);
	}
	private boolean isAuthorized() {
		boolean isAuthorized;
		Client tempClient = Pigeon.createPigeonClient(projectorInfo.getOsVerion(), projectorInfo.getAddress().getHostAddress(), Falcon.EZ_WIFI_DISPLAY_PORT_NUMBER);
		isAuthorized = tempClient.canSendStream();
		Pigeon.releasePigeonClient(tempClient);
		return isAuthorized;
	}
}
