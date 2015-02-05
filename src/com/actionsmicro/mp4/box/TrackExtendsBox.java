package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class TrackExtendsBox extends FullBox {

	private int trackId;
	private int defaultSampleDescriptionIndex;
	private int defaultSampleDuration;
	private int defaultSampleSize;
	private int defaultSampleFlags;
	public TrackExtendsBox(int trackId, int defaultSampleDescriptionIndex, int defaultSampleDuration, int defaultSampleSize, int defaultSampleFlags) {
		super(FourCharCode("trex"), (char)0x00, 0);
		this.trackId = trackId;
		this.defaultSampleDescriptionIndex = defaultSampleDescriptionIndex;
		this.defaultSampleDuration = defaultSampleDuration;
		this.defaultSampleSize = defaultSampleSize;
		this.defaultSampleFlags = defaultSampleFlags;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(trackId);		
		byteBuffer.putInt(defaultSampleDescriptionIndex);		
		byteBuffer.putInt(defaultSampleDuration);		
		byteBuffer.putInt(defaultSampleSize);		
		byteBuffer.putInt(defaultSampleFlags);		
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + 4 + 4 + 4 + 4;
	}
}
