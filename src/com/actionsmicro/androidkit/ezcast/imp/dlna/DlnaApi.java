package com.actionsmicro.androidkit.ezcast.imp.dlna;

import java.net.InetAddress;

import com.actionsmicro.androidkit.ezcast.Api;
import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.TrackableApi;
import com.actionsmicro.androidkit.ezcast.helper.ReferenceCounter;

public class DlnaApi extends TrackableApi implements Api {
	private ApiBuilder<?> apiBuilder;

	public DlnaApi(ApiBuilder<?> apiBuilder) {
		super(apiBuilder);
		this.apiBuilder = apiBuilder;
	}

	private class DlnaClientManager extends ReferenceCounter<DlnaDigitalMediaController, InetAddress> {

		@Override
		protected InetAddress getKey(ApiBuilder<?> apiBuilder) {
			return apiBuilder.getDevice().getIpAddress();
		}

		@Override
		protected DlnaDigitalMediaController createInstance(
				ApiBuilder<?> apiBuilder) {
			DlnaDeviceInfo device = (DlnaDeviceInfo) apiBuilder.getDevice();
			DlnaDigitalMediaController DlnaDigitalMediaController = new DlnaDigitalMediaController(device);
			DlnaDigitalMediaController.connect();
			return DlnaDigitalMediaController;
		}

		@Override
		protected void releaseInstance(DlnaDigitalMediaController apiImp,
				ApiBuilder<?> apiBuilder) {
			apiImp.disconnect();
		}
		
	}
	private DlnaClientManager dlnaClientManager = new DlnaClientManager();
	private DlnaDigitalMediaController dlnaDmc;
	@Override
	public void connect() {
		dlnaDmc = dlnaClientManager.create(apiBuilder);
		super.connect();
	}

	@Override
	public void disconnect() {
		dlnaClientManager.release(dlnaDmc, apiBuilder);
		dlnaDmc = null;
		super.disconnect();
	}

}
