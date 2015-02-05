package com.actionsmicro.analytics.device;

import java.util.Map;

import android.content.Context;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.dlna.DlnaDeviceInfo;

public class DlnaDeviceInfoBuilder extends DeviceInfoBuilder<DlnaDeviceInfo> {

	public DlnaDeviceInfoBuilder(Context context, DlnaDeviceInfo device,
			String appId) {
		super(context, device, appId, "dlna", "2014-12-31", "dlna");
	}
	@Override
	public Map<String, Object> buildDeviceInfo() {
		Map<String, Object> deviceInfo = super.buildDeviceInfo();
		deviceInfo.put("device_id", getDevice().getParameter("deviceid"));
		deviceInfo.put("device_version", getDevice().getParameter("srcvers"));
		deviceInfo.put("manufacturer", getDevice().getManufacturer());
		deviceInfo.put("model", getDevice().getModel());
		
		return deviceInfo;
	}

}
