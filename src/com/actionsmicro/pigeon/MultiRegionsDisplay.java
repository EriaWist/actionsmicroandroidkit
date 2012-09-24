package com.actionsmicro.pigeon;

public interface MultiRegionsDisplay {
	public enum RequestResult {
		UNDIFINED,
		ALLOW,
		DENY,
		NOT_SUPPORTED,
		INVALID_PARAMETER,
		FULL,
		NOT_AVAILABLE
	} 
	public RequestResult requestStreaming(int numberOfRegions, int position) throws Exception;
}
