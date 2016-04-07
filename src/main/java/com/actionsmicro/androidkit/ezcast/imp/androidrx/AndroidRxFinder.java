package com.actionsmicro.androidkit.ezcast.imp.androidrx;


import javax.jmdns.ServiceInfo;

import com.actionsmicro.androidkit.ezcast.DeviceFinder;
import com.actionsmicro.androidkit.ezcast.imp.bonjour.BonjourDeviceFinder;

public class AndroidRxFinder extends BonjourDeviceFinder<AndroidRxInfo> {
	public static final String SERVICE_TYPE = "_ezscreen._tcp.";
	public AndroidRxFinder(DeviceFinder deviceFinderProxy) {
		super(deviceFinderProxy, SERVICE_TYPE);		
	}

	@Override
	public void search(String targetHost) {

	}

	@Override
	protected AndroidRxInfo createFromService(ServiceInfo newService) {
		return new AndroidRxInfo(newService);
	}	
}
