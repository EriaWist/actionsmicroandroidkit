package com.actionsmicro.pigeon;

public class Pigeon {
	public static Client createPigeonClient(final String version, final String serverAddress, int portNumber) {
		if (version.equals("2")) {
			return new ClientV2(serverAddress, portNumber);
		} else {
			return new Client(serverAddress, portNumber);				
		}
	}
}
