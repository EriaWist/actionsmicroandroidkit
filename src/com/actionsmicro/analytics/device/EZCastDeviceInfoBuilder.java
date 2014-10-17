package com.actionsmicro.analytics.device;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;

public class EZCastDeviceInfoBuilder extends DeviceInfoBuilder {

	public EZCastDeviceInfoBuilder(Context context, PigeonDeviceInfo device) {
		super(context, device);
	}

	@Override
	public Object buildDeviceInfo() {
		Map<String, Object> deviceInfo = new HashMap<String, Object>();
		deviceInfo.put("package_id", getPackageId());
		return deviceInfo;
	}

}
