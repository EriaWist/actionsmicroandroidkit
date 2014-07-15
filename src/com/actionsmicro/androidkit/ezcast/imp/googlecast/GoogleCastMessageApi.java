package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import com.actionsmicro.androidkit.ezcast.MessageApi;
import com.actionsmicro.androidkit.ezcast.MessageApiBuilder;

public class GoogleCastMessageApi extends GoogleCastApi implements MessageApi {

	public GoogleCastMessageApi(MessageApiBuilder messageApiBuilder) {
		super(messageApiBuilder);
	}

	@Override
	public void sendKeyAsync(int keyCode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendKeySync(int keyCode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendVendorKey(int keyCode) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void sendJSONRPC(String command) {
	}

}
