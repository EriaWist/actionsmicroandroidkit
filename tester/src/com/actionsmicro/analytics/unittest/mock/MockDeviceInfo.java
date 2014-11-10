package com.actionsmicro.analytics.unittest.mock;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import android.os.Parcel;

import com.actionsmicro.androidkit.ezcast.AuthorizationApi;
import com.actionsmicro.androidkit.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.androidkit.ezcast.MessageApi;
import com.actionsmicro.androidkit.ezcast.MessageApiBuilder;

public class MockDeviceInfo extends DeviceInfo {
	private Map<String, String> parameters = new HashMap<String, String>();
	public MockDeviceInfo(Map<String, String> propertyStrings) {
		this.parameters = propertyStrings;
	}
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub

	}

	@Override
	public InetAddress getIpAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean supportsHttpStreaming() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSplitScreen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsRemoteControl() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsDisplay() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getVendor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getParameter(String key) {
		return parameters.get(key);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected MediaPlayerApi createMediaPlayerApi(
			MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean supportMediaFileExtension(String fileExtension) {
		// TODO Auto-generated method stub
		return false;
	}

}
