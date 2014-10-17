package com.actionsmicro.analytics;

import android.content.Context;

import com.actionsmicro.analytics.device.EZCastDeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;

public abstract class DeviceInfoBuilder {
	private Context context;
	private DeviceInfo device;
	
	public static DeviceInfoBuilder getBuilderForDevice(Context context, com.actionsmicro.androidkit.ezcast.DeviceInfo device) {
		if (device instanceof PigeonDeviceInfo) {
			return new EZCastDeviceInfoBuilder(context, (PigeonDeviceInfo) device);
		}
		return null;
	}
	protected DeviceInfoBuilder(Context context, com.actionsmicro.androidkit.ezcast.DeviceInfo device) {
		this.context = context;
		this.device = device;
	}
	// return a pojo for Gson to convert it to json.
	public abstract Object buildDeviceInfo();
	protected Context getContext() {
		return context;
	}
	protected DeviceInfo getDevice() {
		return device;
	}
	protected String getPackageId() {
		return context.getPackageName();
	}
}
