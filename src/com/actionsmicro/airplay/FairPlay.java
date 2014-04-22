package com.actionsmicro.airplay;

import com.actionsmicro.utils.Log;

public class FairPlay {
	private static final String TAG = "FairPlay";

	static {
        System.loadLibrary("fairplay-jni");
    }
	
	private native static int fp_setup_init();
	private native static byte[] fp_setup_phase1(byte[] data, int size, boolean isAudio);
	private native static byte[] fp_setup_phase2(byte[] data, int size, boolean isAudio);
	private native static byte[] fp_decrypt(byte[] data, int size);
	
	public synchronized static int init() {
		Log.d(TAG, "init");
		int result = fp_setup_init();
		Log.d(TAG, "init done");
		return result;
	}
	public synchronized static byte[] setupPhase1(byte[] data, int size, boolean isAudio) {
		Log.d(TAG, "setupPhase1");
		byte[] result = fp_setup_phase1(data, size, isAudio);
		Log.d(TAG, "setupPhase1 done");
		return result;
	}
	public synchronized static byte[] setupPhase2(byte[] data, int size, boolean isAudio) {
		Log.d(TAG, "setupPhase2");
		byte[] result = fp_setup_phase2(data, size, isAudio);
		Log.d(TAG, "setupPhase2 done");
		return result;
	}
	public synchronized static byte[] decrypt(byte[] data, int size) {
		Log.d(TAG, "decrypt");
		byte[] result = fp_decrypt(data, size);
		Log.d(TAG, "decrypt done");
		return result;
	}
}
