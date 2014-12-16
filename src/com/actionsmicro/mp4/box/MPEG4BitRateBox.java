package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class MPEG4BitRateBox extends Box {
	private int bufferSizeDB; // 0x000294E3
	private int maxBitrate; // 0x00AFFEC8
	private int avgBitrate; // 0x0025C4F8
	public MPEG4BitRateBox() {
		super(FourCharCode("btrt"));
		
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(bufferSizeDB);
		byteBuffer.putInt(maxBitrate);
		byteBuffer.putInt(avgBitrate);		
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + 4 + 4;
	}
}
