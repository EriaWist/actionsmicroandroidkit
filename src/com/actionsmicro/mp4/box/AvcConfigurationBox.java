package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class AvcConfigurationBox extends Box {
	private byte configurationVersion = 1;
	private byte avcProfileIndication; // 100
	private byte profileCompatibility; // 0 
	private byte avcLevelIndication; // 40
	private byte lengthSizeMinusOne= (byte) 0xFF; // in lower 2 bits. 0xFF
	private byte numOfSequenceParameterSets = (byte) (1 | 0xE0); // in lower 5 bits
	private byte[] sps;
	private byte numOfPictureParameterSets = 1;
	private byte[] ps;
	public AvcConfigurationBox(byte avcProfileIndication, byte profileCompatibility, byte avcLevelIndication, byte[] sps, byte[] ps) {
		super(FourCharCode("avcC"));
		this.avcProfileIndication = avcProfileIndication;
		this.profileCompatibility = profileCompatibility;
		this.avcLevelIndication = avcLevelIndication;
		this.sps = sps;
		this.ps = ps;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.put(configurationVersion);
		byteBuffer.put(avcProfileIndication);
		byteBuffer.put(profileCompatibility);
		byteBuffer.put(avcLevelIndication);
		byteBuffer.put(lengthSizeMinusOne);
		byteBuffer.put(numOfSequenceParameterSets);
		byteBuffer.putShort((short)sps.length);
		byteBuffer.put(sps);
		byteBuffer.put(numOfPictureParameterSets);
		byteBuffer.putShort((short)ps.length);
		byteBuffer.put(ps);		
	}
	@Override
	protected int getBodySize() {
		int size = super.getBodySize() + 1 + 1 + 1 + 1 + 1 + 1 + 2 + sps.length + 1 + 2 + ps.length;
		return size;
	}
	
}
