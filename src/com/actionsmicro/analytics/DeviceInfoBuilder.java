package com.actionsmicro.analytics;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.actionsmicro.analytics.device.AirPlayDeviceInfoBuilder;
import com.actionsmicro.analytics.device.EZCastDeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.airplay.AirPlayDeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;

public abstract class DeviceInfoBuilder<T extends DeviceInfo> {
	private Context context;
	private T device;
	private String type;
	private String schemaVersion;
	private String deviceType;
	private String appId;
	
	public static DeviceInfoBuilder<?> getBuilderForDevice(Context context, DeviceInfo device, String appId) {
		if (device instanceof PigeonDeviceInfo) {
			return new EZCastDeviceInfoBuilder(context, (PigeonDeviceInfo) device, appId);
		} else if (device instanceof AirPlayDeviceInfo) {
			return new AirPlayDeviceInfoBuilder(context, (AirPlayDeviceInfo) device, appId);
		}
		
		return null;
	}
	protected DeviceInfoBuilder(Context context, T device, String appId, String type, String schemaVersion, String deviceType) {
		this.context = context;
		this.device = device;
		this.appId = appId;
		this.type = type;
		this.schemaVersion = schemaVersion;
		this.deviceType = deviceType;
	}
	// return a pojo for Gson to convert it to json.
	public Map<String, Object> buildDeviceInfo() {
		Map<String, Object> deviceInfo = new HashMap<String, Object>();
		deviceInfo.put("type", type);
		deviceInfo.put("schema_version", schemaVersion);
		deviceInfo.put("app_id", appId);
		deviceInfo.put("package_id", getPackageId());
		deviceInfo.put("device_type", deviceType);
		return deviceInfo;
	}
	protected Context getContext() {
		return context;
	}
	protected T getDevice() {
		return device;
	}
	private String getPackageId() {
		return context.getPackageName();
	}
}
