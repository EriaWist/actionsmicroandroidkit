package com.actionsmicro.analytics.device;

import java.util.Map;

import android.content.Context;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.airplay.AirPlayDeviceInfo;

public class AirPlayDeviceInfoBuilder extends DeviceInfoBuilder<AirPlayDeviceInfo> {

	public AirPlayDeviceInfoBuilder(Context context, AirPlayDeviceInfo device, String appId) {
		super(context, device, appId, "airplay", "2014-10-24", "airplay");
	}

	@Override
	public Map<String, Object> buildDeviceInfo() {
		Map<String, Object> deviceInfo = super.buildDeviceInfo();
		deviceInfo.put("device_id", getDevice().getPropertyString("deviceid"));
		deviceInfo.put("features", Long.valueOf(getDevice().getPropertyString("features")));
		deviceInfo.put("model", getDevice().getPropertyString("model"));
		deviceInfo.put("srcvers", getDevice().getPropertyString("srcvers"));
		deviceInfo.put("osBuildVersion", getDevice().getPropertyString("osBuildVersion"));
		deviceInfo.put("protovers", getDevice().getPropertyString("protovers"));
		return deviceInfo;
	}

}
