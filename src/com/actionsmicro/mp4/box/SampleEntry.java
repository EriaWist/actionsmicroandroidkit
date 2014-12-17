package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class SampleEntry extends Box {
	private byte reserved[] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

	// data_reference_index is an integer that contains the index of the data reference to use to retrieve
	// data associated with samples that use this sample description. Data references are stored in Data
	// Reference Boxes. The index ranges from 1 to the number of data references
	private short dataReferenceIndex; 
	public SampleEntry(int type, short dataReferenceIndex) {
		super(type);
		this.dataReferenceIndex = dataReferenceIndex;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		for (byte r : reserved) {
			byteBuffer.put(r);
		}
		byteBuffer.putShort(dataReferenceIndex);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 6 + 2;
	}
}
