package com.actionsmicro.androidkit.ezcast;

public interface MessageApi extends Api {
	public interface MessageListener {
		public void onReceiveMessage(MessageApi api, String message);		
	}
	public interface ConnectionManager extends com.actionsmicro.androidkit.ezcast.ConnectionManager {
		public void onDisconnect(MessageApi api);	
	}
	public void sendKeyAsync(int keyCode);
	public void sendKeySync(int keyCode);
	public void sendVendorKey(int keyCode);
	public void sendJSONRPC(String command);
	
}
