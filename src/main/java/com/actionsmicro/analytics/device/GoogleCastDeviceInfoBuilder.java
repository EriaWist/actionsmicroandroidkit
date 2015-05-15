package com.actionsmicro.analytics.device;

import java.util.Map;

import android.content.Context;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.googlecast.GoogleCastDeviceInfo;

public class GoogleCastDeviceInfoBuilder extends
		DeviceInfoBuilder<GoogleCastDeviceInfo> {

	public GoogleCastDeviceInfoBuilder(Context context,
			GoogleCastDeviceInfo device, String appId) {
		super(context, device, appId, "chromecast", "2014-10-24", "chromecast");
	}
	@Override
	public Map<String, Object> buildDeviceInfo() {
		Map<String, Object> deviceInfo = super.buildDeviceInfo();
		deviceInfo.put("device_id", getDevice().getParameter("deviceid"));
		deviceInfo.put("device_version", getDevice().getParameter("srcvers"));
		return deviceInfo;
	}
}
