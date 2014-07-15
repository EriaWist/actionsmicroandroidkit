package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import java.net.InetAddress;

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
import com.actionsmicro.falcon.Falcon.ProjectorInfo;

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
		return new PigeonDisplayApi(displayApiBuilder);
	}
	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		return new PigeonMediaPlayerApi(mediaPlayerApiBuilder);
	}
	@Override
	public boolean supportMediaFileExtension(String fileExtension) {
		return true;
	}
}
