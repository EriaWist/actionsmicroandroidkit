package com.actionsmicro.analytics.device;

import java.util.Map;

import android.content.Context;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxInfo;

public class EZCastScreenDeviceInfoBuilder extends
		DeviceInfoBuilder<AndroidRxInfo> {

	public EZCastScreenDeviceInfoBuilder(Context context, AndroidRxInfo device, String appId) {
		super(context, device, appId, "ezscreen", "2014-10-24", "ezscreen");
	}
	@Override
	public Map<String, Object> buildDeviceInfo() {
		Map<String, Object> deviceInfo = super.buildDeviceInfo();
		deviceInfo.put("device_id", getDevice().getPropertyString("deviceid"));
		deviceInfo.put("srcvers", getDevice().getPropertyString("srcvers"));
		return deviceInfo;
	}
}
