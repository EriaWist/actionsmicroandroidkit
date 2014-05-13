package com.actionsmicro.debug;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.actionsmicro.BuildConfig;

public class DumpBinaryFile {
	private ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
	private FileOutputStream fileout;
	public DumpBinaryFile(String path) throws FileNotFoundException {
		if (BuildConfig.DEBUG) { // only works for debug version, just in case
			fileout = new FileOutputStream(path);
		}
	}
	public void writeToFile(int payloadSize, byte[] pktp) throws IOException {
		if (fileout == null) return;
		lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
		lengthBuffer.clear();
		lengthBuffer.putInt(payloadSize);
		fileout.write(lengthBuffer.array());
		fileout.write(pktp, 0, payloadSize);
	}
	public void close() throws IOException {
		if (fileout != null) {
			fileout.close();
		} else {
			throw new IllegalStateException("no file out defined!");
		}
	}
}
