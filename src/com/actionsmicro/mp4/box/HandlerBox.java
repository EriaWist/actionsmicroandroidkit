package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class HandlerBox extends FullBox {
	private int preDefined = 0;
	private int handlerType = 0;
	private int reserved[] = {0, 0, 0};
	private byte[] name;
	public HandlerBox(int handlerType, String name) {
		super(FourCharCode("hdlr"), (char)0x00, 0);
		this.handlerType = handlerType;
		this.name = name.getBytes(); // in UTF-8
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(preDefined);
		byteBuffer.putInt(handlerType);
		for (int r : reserved) {
			byteBuffer.putInt(r);
		}
		byteBuffer.put(name); 
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + 4 + 3*4 + name.length;
	}
}
