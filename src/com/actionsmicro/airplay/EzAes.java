package com.actionsmicro.airplay;

public class EzAes {
	static {
        System.loadLibrary("fairplay-jni");
    }
	public native static int init(byte[] key, byte[] iv);
	public native static int decrypt(byte[] key, int length, byte[] iv);
	
}
