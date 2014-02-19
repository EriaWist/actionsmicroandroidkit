package com.actionsmicro.ezcast.imp.ezdisplay;

import com.actionsmicro.ezcast.MessageApi;
import com.actionsmicro.ezcast.MessageApiBuilder;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;

public class PigeonMessageApi implements MessageApi {

	private ConnectionManager connectionManager;
	private MessageListener messageListener;
	private ProjectorInfo projector;
	private com.actionsmicro.falcon.Falcon.ProjectorInfo.MessageListener pigeonMessageListener;

	public PigeonMessageApi(MessageApiBuilder apiBuilder) {
		projector = ((PigeonDeviceInfo)apiBuilder.getDevice()).getProjectorInfo();
		connectionManager = apiBuilder.getConnectionManager();
		messageListener = apiBuilder.getMessageListener();
	}

	@Override
	public void connect() {
		projector.addMessageListener(pigeonMessageListener = new ProjectorInfo.MessageListener() {
			
			@Override
			public void onReceiveMessage(ProjectorInfo projector, String message) {
				if (messageListener != null) {
					messageListener.onReceiveMessage(PigeonMessageApi.this, message);
				}
			}
			
			@Override
			public void onException(ProjectorInfo projector, Exception e) {
				if (connectionManager != null) {
					connectionManager.onConnectionFailed(PigeonMessageApi.this, e);
				}
			}
			
			@Override
			public void onDisconnect(ProjectorInfo projector) {
				if (connectionManager != null) {
					connectionManager.onDisconnect(PigeonMessageApi.this);
				}
			}
		});
		
	}

	@Override
	public void disconnect() {
		projector.removeMessageListener(pigeonMessageListener);
		projector.disconnectRemoteControl();
	}

	@Override
	public void sendKeyAsync(int keyCode) {
		projector.sendKey(keyCode);
	}

	@Override
	public void sendKeySync(int keyCode) {
		projector.sendKeyAndWait(keyCode);
	}

	@Override
	public void sendVendorKey(int keyCode) {
		projector.sendVendorKey(keyCode);
	}

}
