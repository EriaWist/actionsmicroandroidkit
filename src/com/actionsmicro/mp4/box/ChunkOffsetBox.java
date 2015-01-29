package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class ChunkOffsetBox extends FullBox {

	public ChunkOffsetBox() {
		super(FourCharCode("stco"), (char) 0, 0);
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
