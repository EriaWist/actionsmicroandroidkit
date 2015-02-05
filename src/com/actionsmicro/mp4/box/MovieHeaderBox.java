package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class MovieHeaderBox extends FullBox {

	private int creationTime;
	private int modificationTime;
	private int timescale = 0x00000258;
	private int duration;
	private int rate = 0x00010000;
	private short volume = 0x0100;
	private short reserved = 0;
	private int reserved2[] = {0, 0};
	private int matrix[] = { 0x00010000,0,0,0,0x00010000,0,0,0,0x40000000 };
	private int preDefined[] = {0,0,0,0,0,0};
	private int nextTrackId = 0;
	public MovieHeaderBox(int flags, int creationＴime, int modificationTime, int nextTrackId, int timescale) {
		super(FourCharCode("mvhd"), (char)0x00, flags);
		this.creationTime = creationＴime;
		this.modificationTime = modificationTime;
		this.nextTrackId = nextTrackId;
		this.timescale = timescale;
	}
	public void setNextTrackId(int nextTrackId) {
		this.nextTrackId = nextTrackId;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(creationTime);
		byteBuffer.putInt(modificationTime);
		byteBuffer.putInt(timescale);
		byteBuffer.putInt(duration);
		byteBuffer.putInt(rate);
		byteBuffer.putShort(volume);
		byteBuffer.putShort(reserved);
		for (int r : reserved2) {
			byteBuffer.putInt(r);
		}
		for (int m : matrix) {
			byteBuffer.putInt(m);
		}
		for (int p : preDefined) {
			byteBuffer.putInt(p);
		}
		byteBuffer.putInt(nextTrackId);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + 4 + 4 + 4 + 4 + 2 + 2 + 2*4 + 9*4 + 6*4 + 4;
	}
}
