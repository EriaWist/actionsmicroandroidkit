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
	public void writeToFile(int payloadSize, int[] input) throws IOException {
		if (fileout == null) return;
		lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
		lengthBuffer.clear();
		lengthBuffer.putInt(payloadSize);
		fileout.write(lengthBuffer.array());
		lengthBuffer.order(ByteOrder.BIG_ENDIAN);
		ByteBuffer buffer = ByteBuffer.allocate(payloadSize * 4);
		for (int i = 0; i < payloadSize; i++) {
			buffer.putInt(input[i]);
		}
		fileout.write(buffer.array());
	}
	public void writeToFile(int payloadSize, short[] input) throws IOException {
		if (fileout == null) return;
		lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
		lengthBuffer.clear();
		lengthBuffer.putInt(payloadSize);
		fileout.write(lengthBuffer.array());
		lengthBuffer.order(ByteOrder.BIG_ENDIAN);
		ByteBuffer buffer = ByteBuffer.allocate(payloadSize * 2);
		for (int i = 0; i < payloadSize; i++) {
			buffer.putShort(input[i]);
		}
		fileout.write(buffer.array());
	}
	public void writeToFile(byte[] byteArray) throws IOException {
		writeToFile(byteArray.length, byteArray);
	}
}
