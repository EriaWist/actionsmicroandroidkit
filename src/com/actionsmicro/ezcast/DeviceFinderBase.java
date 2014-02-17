package com.actionsmicro.ezcast;

import java.util.List;

public abstract class DeviceFinderBase {
	private DeviceFinder deviceFinderProxy;
	public DeviceFinderBase(DeviceFinder deviceFinderProxy) {
		this.deviceFinderProxy = deviceFinderProxy;
	}
	
	protected DeviceFinder getDeviceFinderProxy() {
		return deviceFinderProxy;
	}

	public abstract List<DeviceInfo> getDevices();

	public abstract void stop();

	public abstract void search();
}
