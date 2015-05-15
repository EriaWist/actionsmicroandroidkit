package com.actionsmicro.androidkit.ezcast.imp.airplay;

import javax.jmdns.ServiceInfo;

import com.actionsmicro.androidkit.ezcast.DeviceFinder;
import com.actionsmicro.androidkit.ezcast.imp.bonjour.BonjourDeviceFinder;

public class AirPlayDeviceFinder extends BonjourDeviceFinder<AirPlayDeviceInfo> {
	public static final String SERVICE_TYPE = "_airplay._tcp.";
	public AirPlayDeviceFinder(DeviceFinder deviceFinderProxy) {
		super(deviceFinderProxy, SERVICE_TYPE);		
	}
	@Override
	protected AirPlayDeviceInfo createFromService(ServiceInfo newService) {
		return new AirPlayDeviceInfo(newService);
	}
}
