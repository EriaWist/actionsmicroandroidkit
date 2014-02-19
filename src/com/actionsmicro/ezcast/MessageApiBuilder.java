package com.actionsmicro.ezcast;

import com.actionsmicro.ezcast.MessageApi.*;
import com.actionsmicro.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.ezcast.imp.ezdisplay.PigeonMessageApi;

public class MessageApiBuilder extends ApiBuilder<MessageApi> {

	public MessageApiBuilder(DeviceInfo device) {
		super(device);
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
		if (device instanceof PigeonDeviceInfo) {
			return new PigeonMessageApi(this);
		}
		return null;
	}

	public MessageApi.ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public MessageApiBuilder setConnectionManager(MessageApi.ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		return this;
	}
	
}
