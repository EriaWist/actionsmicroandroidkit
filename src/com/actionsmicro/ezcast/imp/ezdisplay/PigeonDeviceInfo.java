package com.actionsmicro.ezcast.imp.ezdisplay;

import java.net.InetAddress;

import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.ezcast.DeviceInfo;
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
}
