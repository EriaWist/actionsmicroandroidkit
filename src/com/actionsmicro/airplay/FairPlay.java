package com.actionsmicro.airplay;

public class FairPlay {
	static {
        System.loadLibrary("fairplay-jni");
    }
	
	public native static int fp_setup_init();
	public native static byte[] fp_setup_phase1(byte[] data, int size, boolean isAudio);
	public native static byte[] fp_setup_phase2(byte[] data, int size, boolean isAudio);
	public native static byte[] fp_decrypt(byte[] data, int size);
}
