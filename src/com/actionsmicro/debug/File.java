package com.actionsmicro.debug;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class File {
	private ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
	private FileOutputStream fileout;
	public File(String path) throws FileNotFoundException {
		fileout = new FileOutputStream(path);
	}
	public void writeToFile(int payloadSize, byte[] pktp, FileOutputStream fileout) throws IOException {
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
