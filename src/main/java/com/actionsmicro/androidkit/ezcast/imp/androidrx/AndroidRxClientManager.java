package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import java.net.InetAddress;

import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.helper.ReferenceCounter;

public class AndroidRxClientManager extends
		ReferenceCounter<AndroidRxClient, InetAddress> {

	@Override
	protected InetAddress getKey(ApiBuilder<?> apiBuilder) {
		return apiBuilder.getDevice().getIpAddress();
	}

	@Override
	protected AndroidRxClient createInstance(ApiBuilder<?> apiBuilder) {
		AndroidRxInfo device = (AndroidRxInfo) apiBuilder.getDevice();
		AndroidRxClient androidRxClient = new AndroidRxClient(apiBuilder.getContext(), device.getIpAddress(), device.getPort(),device.getParameter("deviceOS"));
		androidRxClient.connect();
		return androidRxClient;
	}

	@Override
	protected void releaseInstance(AndroidRxClient apiImp,
			ApiBuilder<?> apiBuilder) {
		apiImp.disconnect();
	}

}
