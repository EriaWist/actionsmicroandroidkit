package com.actionsmicro.ezcast.imp.googlecast;

import com.actionsmicro.ezcast.MessageApi;
import com.actionsmicro.ezcast.MessageApiBuilder;

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

}
