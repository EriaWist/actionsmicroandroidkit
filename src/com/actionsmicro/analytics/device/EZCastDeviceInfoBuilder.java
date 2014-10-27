package com.actionsmicro.analytics.device;

import java.util.Map;

import android.content.Context;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;

public class EZCastDeviceInfoBuilder extends DeviceInfoBuilder<PigeonDeviceInfo> {

	public EZCastDeviceInfoBuilder(Context context, PigeonDeviceInfo device, String appId) {
		super(context, device, appId, "device", "2014-10-24", "ezcast");
	}

	@Override
	public Map<String, Object> buildDeviceInfo() {
		Map<String, Object> deviceInfo = super.buildDeviceInfo();
		return deviceInfo;
	}

}
