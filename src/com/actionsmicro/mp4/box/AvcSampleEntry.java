package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class AvcSampleEntry extends VisualSampleEntry {

	private AvcConfigurationBox config;
	private MPEG4BitRateBox bitRateBox; // optional
	public AvcSampleEntry(short dataReferenceIndex, short width, short height,
			byte avcProfileIndication, byte profileCompatibility, byte avcLevelIndication, byte sps[], byte ps[]) {
		super(FourCharCode("avc1"), dataReferenceIndex, width, height);
		config = new AvcConfigurationBox(avcProfileIndication, profileCompatibility, avcLevelIndication, sps, ps);
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		config.write(byteBuffer);
		if (bitRateBox != null) {
			bitRateBox.write(byteBuffer);
		}
	}
	@Override
	protected int getBodySize() {
		int size = super.getBodySize() + config.getBoxSize();
		if (bitRateBox != null) {
			size += bitRateBox.getBoxSize();
		}
		return size;
	}

}
