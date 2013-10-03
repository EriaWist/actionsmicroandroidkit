package com.actionsmicro.ezcom;


public interface ConnectionManager {

	void controlConnectionDidFail(Proxy proxy);

	void reverseConnectionDidFail(Proxy proxy);

	void controlConnectionDidDisconnected(Proxy proxy, Exception e);

	void reverseConnectionDidDisconnected(Proxy proxy, Exception e);

	void connectionsEstablishedSuccessfully(Proxy proxy);

	void tryToReconnect(Proxy proxy);

}
