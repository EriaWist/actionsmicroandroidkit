package com.actionsmicro.androidkit.ezcast;

import com.actionsmicro.androidkit.ezcast.MessageApi.MessageListener;

public class MessageApiBuilder extends ApiBuilder<MessageApi> {

	public MessageApiBuilder(EzCastSdk sdk, DeviceInfo device) {
		super(sdk, device);
	}
	
	private MessageListener messageListener; 
	public MessageListener getMessageListener() {
		return messageListener;
	}

	public MessageApiBuilder setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
		return this;
	}
	private MessageApi.ConnectionManager connectionManager;
	
	@Override
	public MessageApi build() {
		return device.createMessageApi(this);
	}

	public MessageApi.ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public MessageApiBuilder setConnectionManager(MessageApi.ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		return this;
	}
	
}
