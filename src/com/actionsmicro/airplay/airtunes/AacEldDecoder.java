package com.actionsmicro.airplay.airtunes;

public class AacEldDecoder {
	static {
        System.loadLibrary("fairplay-jni");
    }
	public native static int init(int frequency, int channel, int constant_duration);
	public native static int decode(byte[] data, int length, byte[] out);
	public native static int release();
}
