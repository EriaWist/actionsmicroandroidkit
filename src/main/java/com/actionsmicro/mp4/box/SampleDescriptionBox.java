package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SampleDescriptionBox extends FullBox {
	private List<SampleEntry> entries = new ArrayList<SampleEntry>();
	public SampleDescriptionBox() {
		super(FourCharCode("stsd"), (char)0x00, 0);
	}
	public void addSampleEntry(SampleEntry sampleEntry) {
		entries.add(sampleEntry);
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(entries.size());
		for (SampleEntry s : entries) {
			s.write(byteBuffer);
		}
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + getEntrySize();
	}
	private int getEntrySize() {
		int size = 0;
		for (SampleEntry s : entries) {
			size += s.getBoxSize();
		}
		return size;
	}

}
