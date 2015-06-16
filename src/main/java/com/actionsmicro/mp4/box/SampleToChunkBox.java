package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class SampleToChunkBox extends FullBox {

	public SampleToChunkBox() {
		super(FourCharCode("stsc"), (char) 0, 0);
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(0);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4;
	}
	
}
