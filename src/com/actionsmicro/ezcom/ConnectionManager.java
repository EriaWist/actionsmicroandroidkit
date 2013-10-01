package com.actionsmicro.ezcom;


public interface ConnectionManager {

	void controlConnectionDidFail(Proxy proxy);

	void reverseConnectionDidFail(Proxy proxy);

	void reverseConnectionDidDisconnected(Proxy proxy, Exception e);


}
