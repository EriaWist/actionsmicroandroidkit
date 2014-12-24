package com.actionsmicro.mp4.box;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaDataBox extends Box {

	private ByteArrayOutputStream buffer;
	public MediaDataBox(int size) {
		super(FourCharCode("mdat"));
		buffer = new ByteArrayOutputStream(size * 2);
		length.order(ByteOrder.BIG_ENDIAN);
	}
	private ByteBuffer length = ByteBuffer.allocate(4);
	private int sliceCount;
	public void addSlice(byte[] slice, int offset, int len) {
		sliceCount++;
		length.clear();
		length.putInt(len);
		try {
			buffer.write(length.array());
			buffer.write(slice, offset, len);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public int getSliceCount() {
		return sliceCount;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.put(buffer.toByteArray());
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + buffer.toByteArray().length;
	}
	public int getDataSize() {
		return buffer.toByteArray().length;
	}
}
