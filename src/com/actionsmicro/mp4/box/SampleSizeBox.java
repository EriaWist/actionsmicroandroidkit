package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class SampleSizeBox extends FullBox {

	public SampleSizeBox() {
		super(FourCharCode("stsz"), (char) 0, 0);
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(0);
		byteBuffer.putInt(0);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 8;
	}
}
