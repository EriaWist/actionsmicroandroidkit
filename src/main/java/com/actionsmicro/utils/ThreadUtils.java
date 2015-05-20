package com.actionsmicro.utils;

public class ThreadUtils {
	public static void stopThreadSafely(Thread thread) {
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
}
