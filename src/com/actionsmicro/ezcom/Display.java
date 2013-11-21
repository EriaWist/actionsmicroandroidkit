package com.actionsmicro.ezcom;

public interface Display {
	public enum Role {
		UNDEFINED, HOST, GUEST
	}
	public interface DisplayListener {
		void remoteRequestToStopDisplaying(Display display);
		
		void positionDidChange(Display display, int splitCount, int position);
		
		void roleDidChange(Display display, Role newRole);
	}
	void startDisplaying();
	void stopDisplaying();
}
