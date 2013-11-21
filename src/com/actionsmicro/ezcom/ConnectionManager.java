package com.actionsmicro.ezcom;


public interface ConnectionManager {

	void controlConnectionDidFail(BasicProxy proxy);

	void reverseConnectionDidFail(BasicProxy proxy);

	void controlConnectionDidDisconnected(BasicProxy proxy, Exception e);

	void reverseConnectionDidDisconnected(BasicProxy proxy, Exception e);

	void connectionsEstablishedSuccessfully(BasicProxy proxy);

	void tryToReconnect(BasicProxy proxy);

}
