package com.actionsmicro.androidkit.ezcast;

import java.util.List;

public abstract class DeviceFinderBase {
	private DeviceFinder deviceFinderProxy;
	public DeviceFinderBase(DeviceFinder deviceFinderProxy) {
		this.deviceFinderProxy = deviceFinderProxy;
	}
	
	protected DeviceFinder getDeviceFinderProxy() {
		return deviceFinderProxy;
	}

	public abstract List<? extends DeviceInfo> getDevices();

	public abstract void stop();

	public abstract void search();

	public abstract void search(String targetHost);
}
