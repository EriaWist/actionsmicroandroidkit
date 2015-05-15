package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class TrackHeaderBox extends FullBox {

	private int creationTime;
	private int modificationTime;
	private int trackId;
	private int reserved = 0;
	private int duration;
	private int reserved2[] = {0, 0};
	private short layer = 0;
	private short alternateGroup = 0;
	private short volume = 0;
	private short reserved3 = 0;
	private int matrix[] = { 0x00010000,0,0,0,0x00010000,0,0,0,0x40000000 };
	private int width;
	private int height;
	
	public TrackHeaderBox(int flags, int creationTime, int modificationTime, int trackId, int duration, int width, int height) {
		super(FourCharCode("tkhd"), (char) 0x00, flags);
		this.creationTime = creationTime;
		this.modificationTime = modificationTime;
		this.trackId = trackId;
		this.duration = duration;
		this.width = width;
		this.height = height;
		
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(creationTime);
		byteBuffer.putInt(modificationTime);
		byteBuffer.putInt(trackId);
		byteBuffer.putInt(reserved);
		byteBuffer.putInt(duration);
		for (int r : reserved2) {
			byteBuffer.putInt(r);
		}
		byteBuffer.putShort(layer);
		byteBuffer.putShort(alternateGroup);
		byteBuffer.putShort(volume);
		byteBuffer.putShort(reserved3);
		for (int m : matrix) {
			byteBuffer.putInt(m);
		}
		byteBuffer.putInt(width);
		byteBuffer.putInt(height);		
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + 4 + 4 + 4 + 4 + 2*4 + 2 + 2 + 2 + 2 + 9*4 + 4 + 4;
	}
}
