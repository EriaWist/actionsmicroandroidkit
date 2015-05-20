package com.actionsmicro.androidkit.ezcast.imp.airplay;

import java.net.InetAddress;

import com.actionsmicro.airplay.AirPlayClient;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.helper.ReferenceCounter;

public class AirPlayClientManager extends ReferenceCounter<AirPlayClient, InetAddress> {

	@Override
	protected AirPlayClient createInstance(ApiBuilder<?> apiBuilder) {
		
		return new AirPlayClient(apiBuilder.getContext(), apiBuilder.getDevice().getIpAddress());
	}

	@Override
	protected void releaseInstance(AirPlayClient apiImp,
			ApiBuilder<?> apiBuilder) {
		apiImp.close();		
	}

	@Override
	protected InetAddress getKey(ApiBuilder<?> apiBuilder) {
		return apiBuilder.getDevice().getIpAddress();
	}

}
