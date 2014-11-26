package com.actionsmicro.androidkit.ezcast.imp.dlna;

import java.io.InputStream;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.ApiBuilder;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.utils.Log;

public class DlnaMediaPlayerApi extends DlnaApi implements MediaPlayerApi {

	protected static final String TAG = "DlnaMediaPlayerApi";
	private Service service;

	public DlnaMediaPlayerApi(ApiBuilder<?> apiBuilder) {
		super(apiBuilder);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean pause() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean resume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean increaseVolume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean decreaseVolume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean seek(int position) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		DlnaDeviceInfo dlnaDevice = (DlnaDeviceInfo)getDevice();
		service = dlnaDevice.getDevice().findService(new UDAServiceId("AVTransport"));
		ActionCallback setAVTransportURIAction = new SetAVTransportURI(service, url, "NO METADATA") {
			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse response, String defaultMsg) {
				Log.e(TAG, defaultMsg);
			}
			@Override
			public void success(ActionInvocation invocation) {
				ActionCallback playAction = new Play(service) {
					@Override
					public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
						// Something was wrong
						Log.e(TAG, defaultMsg);
					}
				};
				UpnpService.getUpnpService().execute(playAction);
			}
		};
		UpnpService.getUpnpService().execute(setAVTransportURIAction);

		return true;
	}

}
