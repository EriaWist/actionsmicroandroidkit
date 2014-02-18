package com.actionsmicro.ezcast;

import java.io.InputStream;

public interface DisplayApi extends Api {
	public enum Role {
		UNDEFINED, HOST, GUEST
	}
	public interface DisplayListener {
		void remoteRequestToStartDisplaying(DisplayApi display, int splitCount, int position);
		
		void remoteRequestToStopDisplaying(DisplayApi display);
		
		void remoteRequestToDisconnect(DisplayApi display);
		
		void positionDidChange(DisplayApi display, int splitCount, int position);
		
		void roleDidChange(DisplayApi display, Role newRole);
	}
	void startDisplaying(); //TODO consider to eliminate this API
	void stopDisplaying(); //TODO consider to eliminate this API
	
	public void resendLastImage() throws Exception;

	public void sendJpegEncodedScreenData(InputStream input, long length) throws Exception;
}
