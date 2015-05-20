package com.actionsmicro.falcon;

import java.io.IOException;
import java.io.InputStream;

public interface DataProvider {
	abstract public InputStream getInputStream() throws IOException;
	abstract public String getStreamType();
}
