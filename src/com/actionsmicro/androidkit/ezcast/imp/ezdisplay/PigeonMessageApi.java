package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import com.actionsmicro.androidkit.ezcast.MessageApi;
import com.actionsmicro.androidkit.ezcast.MessageApiBuilder;
import com.actionsmicro.androidkit.ezcast.TrackableApi;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;

public class PigeonMessageApi extends TrackableApi implements MessageApi {

	private ConnectionManager connectionManager;
	private MessageListener messageListener;
	private ProjectorInfo projector;
	private com.actionsmicro.falcon.Falcon.ProjectorInfo.MessageListener pigeonMessageListener;

	public PigeonMessageApi(MessageApiBuilder apiBuilder) {
		super(apiBuilder);
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
		super.connect();
	}

	@Override
	public void disconnect() {
		projector.removeMessageListener(pigeonMessageListener);
		projector.disconnectRemoteControl();
		super.disconnect();
	}

	@Override
	public void sendKeyAsync(int keyCode) {
		projector.sendKeyTcp(keyCode);
	}

	@Override
	public void sendKeySync(int keyCode) {
		projector.sendKeyTcpAndWait(keyCode);
	}

	@Override
	public void sendVendorKey(int keyCode) {
		projector.sendVendorKey(keyCode);
	}

	@Override
	public void sendJSONRPC(String command) {
		projector.sendJSONRPC(command);
	}

}
