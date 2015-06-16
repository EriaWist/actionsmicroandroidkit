package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class MovieFragmentHeaderBox extends FullBox {
	private int sequenceNumber;
	public MovieFragmentHeaderBox(int sequenceNumber) {
		super(FourCharCode("mfhd"), (char) 0, 0);
		this.sequenceNumber = sequenceNumber;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(sequenceNumber);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4;
	}
}
