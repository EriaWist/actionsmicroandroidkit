package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class MovieExtendsHeaderBox extends FullBox {

	private int fragmentDuration;

	public MovieExtendsHeaderBox(int fragmentDuration) {
		super(FourCharCode("mehd"), (char) 0x00, 0);
		this.fragmentDuration = fragmentDuration;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(fragmentDuration);		
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4;
	}
}
