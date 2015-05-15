package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class MediaHeaderBox extends FullBox {

	private int creationTime;
	private int modificationTime;
	private int timescale = 0x00000258;
	private int duration;
	private short language = 0x55C4; // with most significant padding bit
	private short preDefined = 0;
	
	public MediaHeaderBox(int creationTime, int modificationTime, int timescale, int duration) {
		super(FourCharCode("mdhd"), (char)0x00, 0);
		this.creationTime = creationTime;
		this.modificationTime = modificationTime;
		this.timescale = timescale;
		this.duration = duration;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(creationTime);
		byteBuffer.putInt(modificationTime);
		byteBuffer.putInt(timescale);
		byteBuffer.putInt(duration);
		byteBuffer.putShort(language);
		byteBuffer.putShort(preDefined);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + 4 + 4 + 4 + 2 + 2;
	}
}
