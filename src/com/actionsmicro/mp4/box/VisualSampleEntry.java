package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class VisualSampleEntry extends SampleEntry {
	private short preDefined = 0;
	private short reserved = 0;
	private int preDefined2[] = {0, 0, 0};
	private short width;
	private short height;
	private int horizresolution = 0x00480000;
	private int vertresolution = 0x00480000;
	private int reserved2 = 0;
	private short frameCount = 1;
	private byte name[] = new byte[32];
	private short depth = 0x0018;
	private short preDefined3 = -1;
	public VisualSampleEntry(int type, short dataReferenceIndex, short width, short height) {
		super(type, dataReferenceIndex);
		this.width = width;
		this.height = height;
		
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putShort(preDefined);
		byteBuffer.putShort(reserved);
		for (int r : preDefined2) {
			byteBuffer.putInt(r);
		}
		byteBuffer.putShort(width);
		byteBuffer.putShort(height);
		byteBuffer.putInt(horizresolution);
		byteBuffer.putInt(vertresolution);
		byteBuffer.putInt(reserved2);
		byteBuffer.putShort(frameCount);
		byteBuffer.put(name);
		byteBuffer.putShort(depth);
		byteBuffer.putShort(preDefined3);		
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 2 + 2 + 3*4 + 2 + 2 + 4 + 4 + 4 + 2 + 32 + 2 + 2;
	}
}
