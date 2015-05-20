package com.actionsmicro.falcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class AMCapDDataProvider implements DataProvider {
	private static final int AMCAPD_PORT_NUMBER = 42380;
	@Override
	public InputStream getInputStream() throws IOException {
		Socket socketToAMCapD = new Socket("localhost", AMCAPD_PORT_NUMBER);
		OutputStream socketOutput = (socketToAMCapD.getOutputStream());	
		InputStream socketInput = (socketToAMCapD.getInputStream());	
		// send commend
		socketOutput.write(ByteBuffer.wrap("SCREEN".getBytes("ASCII")).array());
		socketOutput.flush();
		return socketInput;
	}

	@Override
	public String getStreamType() {
		return "com.actionsmicro.amcapd";
	}

}
