package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class TrackRunBox extends FullBox {
	private int firstSampleFlags; //0x00
	private List<Integer> sampleSizes = new ArrayList<Integer>();
	public TrackRunBox(int firstSampleFlags) {
		super(FourCharCode("trun"), (char)0, 0x000204);
		this.firstSampleFlags = firstSampleFlags;
	}
	public TrackRunBox() {
		super(FourCharCode("trun"), (char)0, 0x000200);
	}
	public void addSampleSize(int sampleSize) {
		sampleSizes.add(sampleSize);
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(sampleSizes.size());
		if ((getFlags() & 0x000004) != 0) {
			byteBuffer.putInt(firstSampleFlags);
		}
		for (int sampleSize : sampleSizes) {
			byteBuffer.putInt(sampleSize);			
		}
	}
	@Override
	protected int getBodySize() {
		int size = super.getBodySize() + 4 +getSampleDescSize();
		if ((getFlags() & 0x000004) != 0) {
			size += 4;
		}
		return size;
	}
	private int getSampleDescSize() {
		return 4 * sampleSizes.size();
	}
}
