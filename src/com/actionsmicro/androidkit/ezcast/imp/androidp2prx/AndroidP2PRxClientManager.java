package com.actionsmicro.androidkit.ezcast.imp.androidp2prx;

import java.net.InetAddress;

import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.helper.ReferenceCounter;

public class AndroidP2PRxClientManager extends
		ReferenceCounter<AndroidP2PRxClient, InetAddress> {

	@Override
	protected InetAddress getKey(ApiBuilder<?> apiBuilder) {
		return apiBuilder.getDevice().getIpAddress();
	}

	@Override
	protected AndroidP2PRxClient createInstance(ApiBuilder<?> apiBuilder) {
		AndroidP2PRxInfo device = (AndroidP2PRxInfo) apiBuilder.getDevice();
		AndroidP2PRxClient androidP2PRxClient = new AndroidP2PRxClient(apiBuilder.getContext(), device.getdeviceid(), device.getPort() );
		androidP2PRxClient.connect();
		return androidP2PRxClient;
	}

	@Override
	protected void releaseInstance(AndroidP2PRxClient apiImp,
			ApiBuilder<?> apiBuilder) {
		apiImp.disconnect();
	}

}
