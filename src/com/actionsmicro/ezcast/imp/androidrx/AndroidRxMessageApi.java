package com.actionsmicro.ezcast.imp.androidrx;

import com.actionsmicro.ezcast.MessageApi;
import com.actionsmicro.ezcast.MessageApiBuilder;

public class AndroidRxMessageApi extends AndroidRxApi implements MessageApi {

	public AndroidRxMessageApi(MessageApiBuilder apiBuilder) {
		super(apiBuilder);
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
	public void connect() {
		super.connect();
		this.sendRpcNotification("connect", 0);
	}

	@Override
	public void disconnect() {
		this.sendRpcNotification("disconnect", 3000);
		super.disconnect();
	}
	
	@Override
	public void sendJSONRPC(String command) {
	}

}
