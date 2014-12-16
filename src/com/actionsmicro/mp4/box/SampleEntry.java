package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class SampleEntry extends Box {
	private byte reserved[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
	private short dataReferenceIndex;
	public SampleEntry(int type, short dataReferenceIndex) {
		super(type);
		this.dataReferenceIndex = dataReferenceIndex;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		for (byte r : reserved) {
			byteBuffer.put(r);
		}
		byteBuffer.putShort(dataReferenceIndex);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 6 + 2;
	}
}
