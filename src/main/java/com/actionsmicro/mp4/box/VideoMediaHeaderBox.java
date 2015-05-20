package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class VideoMediaHeaderBox extends FullBox {
	private short graphicsmode = 0;
	private short opcolor[] = {0, 0, 0};
	public VideoMediaHeaderBox() {
		super(FourCharCode("vmhd"), (char)0x00, 0x000001);
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putShort(graphicsmode);
		for (short o : opcolor) {
			byteBuffer.putShort(o);
		}
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 2 + 3*2;
	}
}
