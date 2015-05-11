package com.actionsmicro.p2p;

public interface P2PDeviceListener {

	void onDeviceAdded(String deviceuuid);

	void onDeviceRemoved(String deviceuuid);
		
}