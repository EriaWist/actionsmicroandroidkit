package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class FullBox extends Box {

	private int versionAndFlags;

	public FullBox(int type, char version, int flags) {
		super(type);
		this.versionAndFlags = version << 24;
		this.versionAndFlags |= flags;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(versionAndFlags);
	}
	@Override
	protected int getBodySize() {
		return 4;
	}
	
}
