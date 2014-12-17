package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class TrackFragmentHeaderBox extends FullBox {
	private int trackId;
	private long baseDataOffset;
	public TrackFragmentHeaderBox(int trackId) {
		super(FourCharCode("tfhd"), (char)0, 0x000001);
		this.trackId = trackId;
	}
	public void setBaseDataOffset(long baseDataOffset) {
		this.baseDataOffset = baseDataOffset;		
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(trackId);
		byteBuffer.putLong(baseDataOffset);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + 8;
	}
}
